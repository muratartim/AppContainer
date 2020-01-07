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

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import container.AppContainer;
import container.controller.MainPanel;
import container.data.Settings;
import container.remote.EmbeddedApplication;
import container.utility.Utility;
import javafx.application.Application.Parameters;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;

/**
 * Class for start application task.
 *
 * @author Murat Artim
 * @date 6 May 2018
 * @time 18:11:55
 */
public class StartApplication extends Task<EmbeddedApplication> {

	/** The owner panel. */
	private final MainPanel owner;

	/**
	 * Creates start application task.
	 *
	 * @param owner
	 *            The owner panel.
	 */
	public StartApplication(MainPanel owner) {
		this.owner = owner;
	}

	@Override
	protected EmbeddedApplication call() throws Exception {

		// update info
		updateTitle("Loading & Starting Application");

		// set path to application jar file
		Path jarFile = Utility.getPathToAppJar(AppContainer.APP_DIR);
		if (jarFile == null || !Files.exists(jarFile))
			throw new FileNotFoundException("Cannot find application jar file. Click 'Details' to see a detailed description of the problem.");

		// get URL to jar file
		updateMessage("Building URL to jar path");
		URL[] urls = { jarFile.toUri().toURL() };

		// create class loader
		updateMessage("Creating class loader");
		ClassLoader classLoader = URLClassLoader.newInstance(urls, getClass().getClassLoader());

		// get application main class name retrieve
		updateMessage("Retrieving application main class name");
		String className = Utility.getManifestAttributeValueFromJar(jarFile, "Main-Class");

		// load class
		updateMessage("Loading application main class");
		Class<?> implClass = Class.forName(className, true, classLoader);
		Class<? extends EmbeddedApplication> applicationClass = implClass.asSubclass(EmbeddedApplication.class);
		Constructor<? extends EmbeddedApplication> applicationConstructor = applicationClass.getConstructor(String.class, String.class, String.class, Parameters.class, ClassLoader.class);

		// create application instance
		String appName = (String) owner.getOwner().getSettings().getSetting(Settings.APP_NAME);
		String codeBase = AppContainer.APP_DIR.toString();
		String configFile = AppContainer.CONFIG_FILE.toString();
		Parameters params = owner.getOwner().getParameters();
		EmbeddedApplication application = applicationConstructor.newInstance(appName, codeBase, configFile, params, classLoader);

		// set application to App launcher
		owner.setApplication(application);

		// return application
		return application;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// update info
		updateMessage("Task completed.");
		updateProgress(0, 100);

		try {

			// get application
			EmbeddedApplication application = get();

			// run later
			Platform.runLater(() -> {
				try {

					// initialize application
					application.init();

					// start application
					application.start(new Stage());
				}

				// exception occurred
				catch (Exception e) {

					// notify UI
					owner.taskFailed(e, "Close");

					// log
					AppContainer.LOGGER.log(Level.SEVERE, getClass().getSimpleName() + " has failed.", e);
				}
			});

			// hide launcher stage
			owner.getOwner().getStage().close();
		}

		// exception occurred
		catch (Exception e) {

			// show stage
			if (!owner.getOwner().getStage().isShowing()) {
				owner.getOwner().getStage().show();
			}

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

		// show stage
		if (!owner.getOwner().getStage().isShowing()) {
			owner.getOwner().getStage().show();
		}

		// update info
		updateMessage("Task failed. Click on 'Details' to see a detailed description of the problem.");

		// notify UI
		owner.taskFailed(getException(), "Close");

		// log exception
		AppContainer.LOGGER.log(Level.SEVERE, getClass().getSimpleName() + " has failed.", getException());
	}
}
