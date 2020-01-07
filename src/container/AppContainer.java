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
package container;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import container.controller.MainPanel;
import container.data.Settings;
import container.utility.Utility;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Class for entry point of app container.
 *
 * @author Murat Artim
 * @date 6 May 2018
 * @time 00:23:23
 */
public class AppContainer extends Application {

	/** Launcher version. */
	public static final double VERSION = 1.0;

	/** Operating system type and architecture. */
	public static String OS_TYPE, OS_ARCH;

	/** Resource paths. */
	public static Path LOG_FILE, SETTINGS_FILE, TEMP_DIR, APP_DIR, CONFIG_FILE;

	/** Logger. */
	public static Logger LOGGER;

	/** Primary stage. */
	private Stage stage;

	/** Main panel of the launcher. */
	private MainPanel mainPanel;

	/** App Launcher settings. */
	private Settings settings;

	@Override
	public void init() throws Exception {

		// set operating system type and architecture
		OS_TYPE = Utility.getOSType();
		OS_ARCH = Utility.getOSArch();

		// get launcher codebase
		String codeBase = getHostServices().getCodeBase();

		// create logger
		LOG_FILE = Utility.getPathToFile(codeBase, "appContainer.log");
		LOGGER = Utility.createLogger(Level.INFO);

		// set path to settings files
		SETTINGS_FILE = Utility.getPathToFile(codeBase, "appContainer.set");

		// set path to temporary files directory
		TEMP_DIR = Utility.getPathToFile(codeBase, "tempdir");
		TEMP_DIR = Files.exists(TEMP_DIR) ? TEMP_DIR : Files.createDirectory(TEMP_DIR);

		// set path to application directory
		APP_DIR = Utility.getPathToFile(codeBase, "appdir");
		APP_DIR = Files.exists(APP_DIR) ? APP_DIR : Files.createDirectory(APP_DIR);

		// log
		LOGGER.info("App Container v" + VERSION + " initialized.");
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		// load settings
		settings = Settings.loadSettings();
		LOGGER.info("App container settings loaded.");

		// set path to launch configuration file
		String codeBase = getHostServices().getCodeBase();
		String appName = (String) settings.getSetting(Settings.APP_NAME);
		CONFIG_FILE = Utility.getPathToLaunchConfigurationFile(codeBase, appName + ".cfg");

		// clean temporary directory
		Utility.deleteTemporaryFiles(TEMP_DIR, TEMP_DIR);
		LOGGER.info("Temporary directory cleaned.");

		// set stage undecorated
		primaryStage.initStyle(StageStyle.TRANSPARENT);
		primaryStage.setResizable(false);

		// set primary stage
		stage = primaryStage;

		// create main panel
		mainPanel = MainPanel.load(this);

		// create scene
		Scene scene = new Scene(mainPanel.getRoot());
		scene.setFill(Color.TRANSPARENT);

		// setup stage
		stage.setScene(scene);
		stage.setTitle(settings.getSetting(Settings.APP_NAME) + " Container");
		stage.getIcons().add(new Image("container/image/icon.png"));
		stage.hide();

		// start main panel
		mainPanel.start();

		// log
		LOGGER.info("App container started.");
	}

	@Override
	public void stop() throws Exception {

		// log
		LOGGER.info("App container stopped.");

		// close logger
		Arrays.stream(LOGGER.getHandlers()).forEach(h -> h.close());

		// stop main panel
		mainPanel.stop();

		// exit
		System.exit(0);
	}

	/**
	 * Returns App Container settings.
	 *
	 * @return App Container settings.
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Returns the primary stage.
	 *
	 * @return The primary stage.
	 */
	public Stage getStage() {
		return stage;
	}

	/**
	 * Sets settings.
	 *
	 * @param settings
	 *            Settings.
	 */
	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	/**
	 * The main() method is ignored in correctly deployed JavaFX application. main() serves only as fallback in case the application can not be launched through deployment artifacts, e.g., in IDEs with limited FX support. NetBeans ignores main().
	 *
	 * @param args
	 *            The command line arguments.
	 * @throws IOException
	 *             If exception occurs during launch.
	 */
	public static void main(String[] args) throws IOException {
		launch(args);
	}
}
