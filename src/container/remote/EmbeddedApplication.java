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
package container.remote;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import container.AppContainer;
import container.utility.Utility;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;

/**
 * Abstract class for embedded application.
 *
 * @author Murat Artim
 * @date 8 May 2018
 * @time 00:50:30
 */
public abstract class EmbeddedApplication extends Application {

	/** Path to application directory and launch configuration file. */
	private final String appName, appDir, configFile;

	/** Application parameters. */
	private final Parameters parameters;

	/**
	 * No argument constructor. This constructor should be used when the application is not wrapped inside the <code>AppContainer</code> (i.e. no auto-update mechanism is employed).
	 */
	public EmbeddedApplication() {
		super();
		appName = null;
		appDir = null;
		configFile = null;
		parameters = null;
	}

	/**
	 * Creates embedded application.
	 *
	 * @param appName
	 *            Application name.
	 * @param appDir
	 *            Path to application directory.
	 * @param configFile
	 *            Path to launch configuration file.
	 * @param parameters
	 *            Application parameters.
	 * @param classLoader
	 *            Class loader to set to FXML loaders.
	 */
	public EmbeddedApplication(String appName, String appDir, String configFile, Parameters parameters, ClassLoader classLoader) {

		// create application
		super();

		// set application name and directory path
		this.appName = appName;
		this.appDir = appDir;

		// set launch configuration file
		this.configFile = configFile;

		// register startup parameters to application
		this.parameters = parameters;

		// set default class loader for FXML loaders
		FXMLLoader.setDefaultClassLoader(classLoader);
	}

	/**
	 * Returns the application name, or <code>null</code> if the application is not wrapped inside the <code>AppContainer</code>.
	 *
	 * @return The application name, or <code>null</code> if the application is not wrapped inside the <code>AppContainer</code>.
	 */
	public String getAppName() {
		return appName;
	}

	/**
	 * Returns the path to application directory, or <code>null</code> if the application is not wrapped inside the <code>AppContainer</code>.
	 *
	 * @return Path to application directory, or <code>null</code> if the application is not wrapped inside the <code>AppContainer</code>.
	 */
	public String getAppDir() {
		return appDir;
	}

	/**
	 * Returns the path to launch configuration file (.cfg), or <code>null</code> if the application is not wrapped inside the <code>AppContainer</code>.
	 *
	 * @return The path to launch configuration file (.cfg), or <code>null</code> if the application is not wrapped inside the <code>AppContainer</code>.
	 */
	public String getLaunchConfigurationFile() {
		return configFile;
	}

	/**
	 * Returns application parameters. Note that, this method should called instead of <code>getParameters()</code>.
	 *
	 * @return Application parameters.
	 */
	public Parameters getApplicationParameters() {
		return parameters == null ? getParameters() : parameters;
	}

	/**
	 * Returns path to application code base. Note that, this method should called instead of <code>getHostServices().getCodeBase()</code>.
	 *
	 * @return Path to application code base.
	 * @throws URISyntaxException
	 *             If exception occurs during process.
	 */
	public Path getCodeBase() throws URISyntaxException {

		// get application code base
		String codeBase = getHostServices().getCodeBase();

		// null or empty codebase string
		if (codeBase == null || codeBase.isEmpty()) {

			// no application directory name specified
			if (appDir == null)
				return Paths.get("");

			// application directory name specified
			return Paths.get(appDir);
		}

		// no application directory name specified
		if (appDir == null)
			return Paths.get(new URI(codeBase));

		// application directory name specified
		return Paths.get(new URI(codeBase)).resolve(appDir);
	}

	/**
	 * Restarts App container.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public void restartContainer() throws Exception {
		Utility.restartContainer(appName);
	}

	/**
	 * Return the current version of the container.
	 *
	 * @return The current version of the container.
	 */
	public static double getContainerVersion() {
		return AppContainer.VERSION;
	}
}
