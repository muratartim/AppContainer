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
 * Class for download application resources task.
 *
 * @author Murat Artim
 * @date 6 May 2018
 * @time 23:41:45
 */
public class DownloadAppResources extends Task<ArrayList<Path>> implements DownloadListener, SftpProgressMonitor {

	/** The owner panel. */
	private final MainPanel owner;

	/** Application resources to delete. */
	private final ArrayList<ApplicationResource> resources;

	/** Download progress parameters. */
	private long count = 0, max = 0, percent = -1;

	/**
	 * Creates download application resources task.
	 *
	 * @param owner
	 *            The owner panel.
	 * @param resources
	 *            Application resources to delete from local application directory.
	 */
	public DownloadAppResources(MainPanel owner, ArrayList<ApplicationResource> resources) {
		this.owner = owner;
		this.resources = resources;
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

	@Override
	protected ArrayList<Path> call() throws Exception {

		// update info
		updateTitle("Downloading Application Resources");
		updateProgress(0, 100);

		// create list
		ArrayList<Path> downloadedResources = new ArrayList<>();

		// get settings
		Settings settings = owner.getOwner().getSettings();

		// download from SFTP server
		if (settings.getSetting(Settings.HOSTING_TYPE).equals(Settings.SFTP_HOSTING)) {

			// create server connection
			try (SFTPConnection connection = Utility.createSFTPConnection(settings)) {

				// loop over resources
				for (ApplicationResource resource : resources) {

					// update info
					String resourceName = resource.toString();
					updateMessage("Downloading application resource '" + resourceName + "'. This may take a few seconds.");

					// download resource
					Path destination = AppContainer.TEMP_DIR.resolve(resourceName);
					connection.getSftpChannel().get(resource.getPath(), destination.toString(), this);

					// add to list
					downloadedResources.add(destination);

					// reset progress
					count = 0;
					max = 0;
					percent = -1;
				}

				// download manifest file (if it doesn't exist)
				Path localManifest = AppContainer.TEMP_DIR.resolve("MANIFEST.MF");
				if (!Files.exists(localManifest)) {
					connection.getSftpChannel().get((String) settings.getSetting(Settings.MANIFEST_LOCATION), localManifest.toString(), this);
				}
			}
		}

		// download from web server
		else if (settings.getSetting(Settings.HOSTING_TYPE).equals(Settings.WEB_HOSTING)) {

			// loop over resources
			for (ApplicationResource resource : resources) {

				// update info
				String resourceName = resource.toString();
				updateMessage("Downloading application resource '" + resourceName + "'. This may take a few seconds.");

				// download resource
				Path destination = AppContainer.TEMP_DIR.resolve(resourceName);
				Utility.download(destination.toString(), new URL(resource.getPath()), this);

				// add to list
				downloadedResources.add(destination);
			}

			// download manifest file (if it doesn't exist)
			Path localManifest = AppContainer.TEMP_DIR.resolve("MANIFEST.MF");
			if (!Files.exists(localManifest)) {
				Utility.download(localManifest.toString(), new URL((String) settings.getSetting(Settings.MANIFEST_LOCATION)), this);
			}
		}

		// return paths to downloaded resources
		return downloadedResources;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();
		updateProgress(0, 100);

		// update info
		updateMessage("Task completed.");

		try {

			// get downloaded resources
			ArrayList<Path> downloadedResources = get();

			// start load application task
			owner.startTask(new ExtractAppResources(owner, downloadedResources));
		}

		// exception occurred
		catch (Exception e) {

			// update progress
			updateProgress(0, 100);

			// notify UI
			owner.taskFailed(e, "Close");

			// log
			AppContainer.LOGGER.log(Level.SEVERE, getClass().getSimpleName() + " has failed.", e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();
		updateProgress(0, 100);

		// update info
		updateMessage("Task failed. Click on 'Details' to see a detailed description of the problem.");

		// notify UI
		owner.taskFailed(getException(), "Close");

		// log exception
		AppContainer.LOGGER.log(Level.SEVERE, getClass().getSimpleName() + " has failed.", getException());
	}
}
