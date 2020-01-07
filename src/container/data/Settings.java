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
package container.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

import container.AppContainer;

/**
 * Class for app container settings.
 *
 * @author Murat Artim
 * @date 8 May 2018
 * @time 11:52:36
 */
public class Settings implements Serializable {

	/** Serial id. */
	private static final long serialVersionUID = 1L;

	/** Application hosting type. */
	public static final String WEB_HOSTING = "Web Hosting", SFTP_HOSTING = "SFTP Hosting";

	/** Setting index. */
	// @formatter:off
	public static final int HOSTING_TYPE = 0, APP_NAME = 1, VERSION_DESC_URL = 2, MANIFEST_LOCATION = 3, CONNECTION_TIMEOUT = 4, SFTP_HOSTNAME = 5,
			SFTP_PORT = 6, SFTP_USERNAME = 7, SFTP_PASSWORD = 8, APP_RESOURCES = 9, MANIFEST_ATTRIBUTE_FOR_UPDATE_NOTIFICATION = 10, MANIFEST_ATTRIBUTE_FOR_IGNORE_UPDATE_ALLOWANCE = 11;
	// @formatter:on

	/** Settings mapping. */
	private final HashMap<Integer, Object> settings;

	/**
	 * Creates App Launcher settings with default values.
	 */
	public Settings() {
		settings = new HashMap<>();
		setDefaultValues();
	}

	/**
	 * Returns App Launcher settings.
	 *
	 * @return App Launcher settings.
	 */
	public HashMap<Integer, Object> getSettings() {
		return settings;
	}

	/**
	 * Returns setting value.
	 *
	 * @param index
	 *            Setting index.
	 * @return Setting value.
	 */
	public Object getSetting(int index) {
		return settings.get(index);
	}

	/**
	 * Puts setting value.
	 *
	 * @param index
	 *            Setting index.
	 * @param value
	 *            Setting value.
	 * @return The previous value of the setting, or null if there was no previous value set.
	 */
	public Object put(int index, Object value) {
		return settings.put(index, value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (settings == null ? 0 : settings.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Settings other = (Settings) obj;
		if (settings == null) {
			if (other.settings != null)
				return false;
		}
		else if (!settings.equals(other.settings))
			return false;
		return true;
	}

	/**
	 * Saves this settings object to settings file.
	 *
	 * @param settingsFile
	 *            Path to settings file to save the settings.
	 */
	public void saveSettings(Path settingsFile) {
		try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(settingsFile.toFile())))) {
			out.writeObject(this);
		}
		catch (Exception e) {
			AppContainer.LOGGER.log(Level.WARNING, "Exception occurred during saving App Launcher settings.", e);
		}
	}

	/**
	 * Loads App Launcher settings from file, or loads default settings if settings file doesn't exist.
	 *
	 * @return App Launcher settings.
	 */
	public static Settings loadSettings() {

		// no settings file
		if (!AppContainer.SETTINGS_FILE.toFile().exists())
			return new Settings();

		// read settings file
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(AppContainer.SETTINGS_FILE.toFile())))) {
			return (Settings) in.readObject();
		}

		// exception occurred during reading
		catch (Exception e) {
			AppContainer.LOGGER.log(Level.WARNING, "Exception occurred during loading App Launcher settings.", e);
			return new Settings();
		}
	}

	/**
	 * Sets default values of settings.
	 */
	private void setDefaultValues() {

		// set root path
		String rootPath = "http://www.equinox-digital-twin.com/files/";

		// set standard settings
		settings.put(Settings.HOSTING_TYPE, Settings.WEB_HOSTING);
		settings.put(Settings.APP_NAME, "Equinox Digital Twin");
		settings.put(Settings.VERSION_DESC_URL, rootPath.concat("versionDescription.html"));
		settings.put(Settings.MANIFEST_LOCATION, rootPath.concat("MANIFEST.MF"));
		settings.put(Settings.CONNECTION_TIMEOUT, "3000");
		settings.put(Settings.MANIFEST_ATTRIBUTE_FOR_UPDATE_NOTIFICATION, "Notify-Update");
		settings.put(Settings.MANIFEST_ATTRIBUTE_FOR_IGNORE_UPDATE_ALLOWANCE, "Allow-Ignore-Update");

		// set application resources
		ArrayList<ApplicationResource> appResources = new ArrayList<>();
		ApplicationResource jar = new ApplicationResource();
		jar.setPath(rootPath.concat("jar.zip"));
		jar.setManifestAttribute("Jar-Version");
		jar.setFileNames(new ArrayList<>(Arrays.asList("Equinox.jar")));
		appResources.add(jar);
		ApplicationResource libs = new ApplicationResource();
		libs.setPath(rootPath.concat("libs.zip"));
		libs.setManifestAttribute("Lib-Version");
		libs.setFileNames(new ArrayList<>(Arrays.asList("libs")));
		appResources.add(libs);
		ApplicationResource resources = new ApplicationResource();
		resources.setPath(rootPath.concat("resources.zip"));
		resources.setManifestAttribute("Resource-Version");
		resources.setFileNames(new ArrayList<>(Arrays.asList("resources")));
		appResources.add(resources);
		settings.put(Settings.APP_RESOURCES, appResources);
	}
}
