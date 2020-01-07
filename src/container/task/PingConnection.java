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

import java.util.logging.Level;

import container.AppContainer;
import container.controller.MainPanel;
import container.data.Settings;
import container.utility.Utility;
import javafx.concurrent.Task;

/**
 * Class for ping server connection task.
 *
 * @author Murat Artim
 * @date 12 May 2018
 * @time 18:06:30
 */
public class PingConnection extends Task<Void> {

	/** The owner panel. */
	private final MainPanel owner;

	/** True to allow skipping update. */
	private boolean allowSkippingUpdate;

	/**
	 * Creates ping server connection task.
	 *
	 * @param owner
	 *            The owner panel.
	 */
	public PingConnection(MainPanel owner) {
		this.owner = owner;
	}

	@Override
	protected Void call() throws Exception {

		// update info
		updateTitle("Ping Server Connection");

		// check if skipping update can be allowed
		allowSkippingUpdate = Utility.getPathToAppJar(AppContainer.APP_DIR) != null;

		// get settings
		Settings settings = owner.getOwner().getSettings();

		// ping SFTP server
		if (settings.getSetting(Settings.HOSTING_TYPE).equals(Settings.SFTP_HOSTING)) {
			Utility.pingSFTPConnection(settings);
		}

		// ping web server
		else if (settings.getSetting(Settings.HOSTING_TYPE).equals(Settings.WEB_HOSTING)) {
			String url = (String) settings.getSetting(Settings.MANIFEST_LOCATION);
			int timeout = Integer.parseInt((String) settings.getSetting(Settings.CONNECTION_TIMEOUT));
			if (!Utility.pingURL(url, timeout))
				throw new Exception("Web server is not reachable.");
		}

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();
		updateProgress(0, 100);

		// update info
		updateMessage("Server ping succeeded. The host server is reacheable. Proceeding with update check...");

		// start load application task
		owner.startTask(new CheckForUpdates(owner));
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();
		updateProgress(0, 100);

		// show stage
		owner.getOwner().getStage().show();

		// update info
		updateMessage("Cannot connect to host server. Click on 'Details' to see a detailed description of the problem.");

		// notify UI
		owner.taskFailed(getException(), allowSkippingUpdate ? "Skip Update" : "Close");

		// log exception
		AppContainer.LOGGER.log(Level.SEVERE, getClass().getSimpleName() + " has failed.", getException());
	}
}
