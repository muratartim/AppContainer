/*
 * Copyright 2018 Murat Artim (muratartim@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package container.task;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.jcraft.jsch.SftpProgressMonitor;

import container.AppContainer;
import container.controller.MainPanel;
import container.data.ApplicationResource;
import container.data.Settings;
import container.utility.DownloadListener;
import container.utility.RBCWrapper;
import container.utility.SFTPConnection;
import container.utility.Utility;
import javafx.concurrent.Task;

/**
 * Class for check for updates task.
 *
 * @author Murat Artim
 * @date 7 May 2018
 * @time 11:10:18
 */
public class CheckForUpdates extends Task<ArrayList<ApplicationResource>> implements DownloadListener, SftpProgressMonitor {

	/** The owner panel. */
	private final MainPanel owner;

	/** True to allow skipping update. */
	private boolean localManifestExists = false, notifyUpdate = false, allowSkippingUpdate = true;

	/** Download progress parameters. */
	private long count = 0, max = 0, percent = -1;

	/**
	 * Creates check for updates task.
	 *
	 * @param owner
	 *            The owner panel.
	 */
	public CheckForUpdates(MainPanel owner) {
		this.owner = owner;
	}

	@Override
	public void setDownloadProgress(RBCWrapper rbc, double progress) {
		updateProgress(progress, 100.0);
	}

	@Override
	public boolean count(long count) {
		this.count += count;
		if (percent >= this.count * 100 / max)
			return true;
		percent = this.count * 100 / max;
		updateProgress(percent, 100);
		return true;
	}

	@Override
	public void end() {
		// no implementation
	}

	@Override
	public void init(int op, String src, String dest, long max) {
		count = 0;
		percent = -1;
		this.max = max;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ArrayList<ApplicationResource> call() throws Exception {

		// update info
		updateTitle("Checking For Updates");
		updateProgress(0, 100);

		// get application settings
		Settings settings = owner.getOwner().getSettings();

		// get local manifest file
		Path localManifest = Utility.getPathToAppManifest(AppContainer.APP_DIR);
		if (localManifest == null || !Files.exists(localManifest))
			return (ArrayList<ApplicationResource>) settings.getSetting(Settings.APP_RESOURCES);
		localManifestExists = true;

		// get resource versions from local manifest file
		HashMap<String, String> localResourceVersions = Utility.getResourceVersionsFromManifest(localManifest, settings);
		if (localResourceVersions == null || localResourceVersions.isEmpty())
			return (ArrayList<ApplicationResource>) settings.getSetting(Settings.APP_RESOURCES);

		// download remote manifest file
		updateMessage("Downloading application manifest file from server. This may take a few seconds.");
		Path remoteManifest = AppContainer.TEMP_DIR.resolve("MANIFEST.MF");

		// download from SFTP server
		if (settings.getSetting(Settings.HOSTING_TYPE).equals(Settings.SFTP_HOSTING)) {
			try (SFTPConnection connection = Utility.createSFTPConnection(settings)) {
				connection.getSftpChannel().get((String) settings.getSetting(Settings.MANIFEST_LOCATION), remoteManifest.toString(), this);
			}
		}

		// download from web server
		else if (settings.getSetting(Settings.HOSTING_TYPE).equals(Settings.WEB_HOSTING)) {
			Utility.download(remoteManifest.toString(), new URL((String) settings.getSetting(Settings.MANIFEST_LOCATION)), this);
		}

		// remote manifest file doesn't exist
		if (!Files.exists(remoteManifest))
			return null;

		// get update notification attribute from manifest
		String notify = Utility.getManifestAttributeValueFromManifest(remoteManifest, (String) settings.getSetting(Settings.MANIFEST_ATTRIBUTE_FOR_UPDATE_NOTIFICATION));
		notifyUpdate = notify == null ? false : Boolean.parseBoolean(notify);

		// get ignore update allowance attribute from manifest
		String skip = Utility.getManifestAttributeValueFromManifest(remoteManifest, (String) settings.getSetting(Settings.MANIFEST_ATTRIBUTE_FOR_IGNORE_UPDATE_ALLOWANCE));
		allowSkippingUpdate = skip == null ? false : Boolean.parseBoolean(skip);

		// get resource versions from remote manifest file
		HashMap<String, String> remoteResourceVersions = Utility.getResourceVersionsFromManifest(remoteManifest, settings);
		if (remoteResourceVersions == null || remoteResourceVersions.isEmpty())
			return null;

		// compare resource versions
		ArrayList<ApplicationResource> toBeUpdated = new ArrayList<>();
		ArrayList<ApplicationResource> resources = (ArrayList<ApplicationResource>) settings.getSetting(Settings.APP_RESOURCES);
		for (ApplicationResource resource : resources) {
			String manifestAttribute = resource.getManifestAttribute();
			String localValue = localResourceVersions.get(manifestAttribute);
			String remoteValue = remoteResourceVersions.get(manifestAttribute);
			if (localValue == null || !localValue.equals(remoteValue)) {
				toBeUpdated.add(resource);
			}
		}

		// return application resources to be updated
		return toBeUpdated.isEmpty() ? null : toBeUpdated;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();
		updateProgress(0, 100);

		try {

			// get application resources to be updated
			ArrayList<ApplicationResource> toBeUpdated = get();

			// get application name
			String appName = (String) owner.getOwner().getSettings().getSetting(Settings.APP_NAME);

			// start application
			if (toBeUpdated == null) {
				updateMessage("Your " + appName + " is up to date! No new update is available.");
				owner.startTask(new StartApplication(owner));
				return;
			}

			// show update available
			if (notifyUpdate) {
				owner.getOwner().getStage().show();
				updateTitle("Update Available");
				String message = "A newer version of " + appName + " is available. ";
				message += "Click 'Install' to upgrade your software.";
				updateMessage(message);
				owner.updateAvailable(localManifestExists && allowSkippingUpdate, toBeUpdated);
				return;
			}

			// update application
			owner.getOwner().getStage().show();
			updateTitle("Update Available");
			String message = "A newer version of " + appName + "  is available. ";
			message += "Click 'Install' to upgrade your software.";
			updateMessage(message);
			owner.startTask(new DeleteAppResources(owner, toBeUpdated));
		}

		// exception occurred
		catch (Exception e) {

			// show stage
			owner.getOwner().getStage().show();

			// update progress
			updateProgress(0, 100);

			// notify UI
			owner.taskFailed(e, localManifestExists && allowSkippingUpdate ? "Skip Update" : "Close");

			// log
			AppContainer.LOGGER.log(Level.SEVERE, getClass().getSimpleName() + " has failed.", e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();
		updateProgress(0, 100);

		// show stage
		owner.getOwner().getStage().show();

		// update info
		updateMessage("Task failed. Click on 'Details' to see a detailed description of the problem.");

		// notify UI
		owner.taskFailed(getException(), localManifestExists && allowSkippingUpdate ? "Skip Update" : "Close");

		// log exception
		AppContainer.LOGGER.log(Level.SEVERE, getClass().getSimpleName() + " has failed.", getException());
	}
}
