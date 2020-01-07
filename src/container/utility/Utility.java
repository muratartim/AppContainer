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
package container.utility;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.text.WordUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import container.AppContainer;
import container.controller.MainPanel;
import container.controller.SettingsHeader;
import container.controller.SettingsPanel;
import container.data.ApplicationResource;
import container.data.Settings;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/**
 * Class for utility methods.
 *
 * @author Murat Artim
 * @date 6 May 2018
 * @time 00:47:29
 */
public class Utility {

	/** Operating system name and architecture. */
	public static final String MACOS = "macos", WINDOWS = "windows", LINUX = "linux", X86 = "x86", X64 = "x64";

	/**
	 * Returns the operating system type.
	 *
	 * @return The operating system type.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static String getOSType() throws Exception {

		// get OS name
		String osName = System.getProperty("os.name");

		// Mac OS X
		if (osName.contains("Mac OS X"))
			return MACOS;

		// Windows
		else if (osName.contains("Windows"))
			return WINDOWS;

		// Linux
		else if (osName.contains("Linux"))
			return LINUX;

		// unrecognized OS
		return null;
	}

	/**
	 * Returns the operating system architecture.
	 *
	 * @return The operating system architecture.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static String getOSArch() throws Exception {

		// get architecture
		String osArch = System.getProperty("os.arch");

		// 64 bit
		if (osArch.contains("64"))
			return X64;

		// 32 bit
		return X86;
	}

	/**
	 * Shows settings dialog.
	 *
	 * @param mainPanel
	 *            Main panel.
	 */
	public static void showSettingsDialog(MainPanel mainPanel) {

		// get settings
		Settings settings = mainPanel.getOwner().getSettings();

		// create dialog
		Dialog<Settings> dialog = new Dialog<>();
		String launcherName = mainPanel.getOwner().getSettings().getSetting(Settings.APP_NAME) + " Container";
		dialog.setTitle(launcherName + " Settings");
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image("container/image/icon.png"));
		dialog.setGraphic(new ImageView(new Image("container/image/settingsBig.png")));
		dialog.setResizable(false);

		// set dialog header
		SettingsHeader header = SettingsHeader.load((String) settings.getSetting(Settings.HOSTING_TYPE));
		dialog.getDialogPane().setHeader(header.getRoot());

		// create and add settings panel
		SettingsPanel settingsPanel = SettingsPanel.load(settings, header);
		dialog.getDialogPane().setContent(settingsPanel.getRoot());

		// add buttons to dialog
		ButtonType apply = new ButtonType("Apply", ButtonData.APPLY);
		dialog.getDialogPane().getButtonTypes().add(apply);
		ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		dialog.getDialogPane().getButtonTypes().add(cancel);
		ButtonType reset = new ButtonType("Reset", ButtonData.LEFT);
		dialog.getDialogPane().getButtonTypes().add(reset);

		// setup reset button action
		final Button resetButton = (Button) dialog.getDialogPane().lookupButton(reset);
		resetButton.addEventFilter(ActionEvent.ACTION, event -> {
			settingsPanel.setFromSettings(new Settings());
			event.consume();
		});

		// result converter for dialog
		dialog.setResultConverter(b -> {

			// apply
			if (b.equals(apply))
				return settingsPanel.createSettings();

			// reset
			return null;
		});

		// show dialog
		Optional<Settings> result = dialog.showAndWait();

		// save settings
		if (result.isPresent()) {

			try {

				// get new settings
				Settings newSettings = result.get();

				// there are changes
				if (!mainPanel.getOwner().getSettings().equals(newSettings)) {

					// save settings
					newSettings.saveSettings(AppContainer.SETTINGS_FILE);
					mainPanel.getOwner().setSettings(newSettings);

					// ask to restart
					showRestartDialog(mainPanel);
				}
			}

			// exception occurred
			catch (Exception e) {

				// log exception
				AppContainer.LOGGER.log(Level.WARNING, "Exception occurred during saving " + launcherName + " settings.", e);

				// notify UI
				mainPanel.showError("Save Settings", "Exception occurred during saving " + launcherName + " settings.", e, "Close");
			}
		}
	}

	/**
	 * Shows restart App Launcher dialog.
	 *
	 * @param mainPanel
	 *            main panel.
	 */
	public static void showRestartDialog(MainPanel mainPanel) {

		// create confirmation dialog
		Alert alert = new Alert(AlertType.CONFIRMATION);
		String appName = (String) mainPanel.getOwner().getSettings().getSetting(Settings.APP_NAME);
		String containerName = appName + " Container";
		alert.setTitle("Restart " + containerName);
		String message = "Changes to settings will take effect after restart of " + containerName + ". Do you want to restart " + containerName + " now?";
		alert.setHeaderText(WordUtils.wrap(message, 50));
		alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image("container/image/icon.png"));

		// show dialog
		Optional<ButtonType> result = alert.showAndWait();

		// yes clicked
		if (result.isPresent() && result.get().equals(ButtonType.YES)) {

			try {

				// restart launcher
				restartContainer(appName);

				// exit
				Platform.exit();
			}

			// exception occurred
			catch (Exception e) {

				// log exception
				AppContainer.LOGGER.log(Level.WARNING, "Exception occurred during restarting " + containerName + ".", e);

				// notify UI
				mainPanel.showError("Restart " + containerName, "Exception occurred during restarting " + containerName + ".", e, "Close");
			}
		}
	}

	/**
	 * Returns path to launch configuration file.
	 *
	 * @param codeBase
	 *            Code base attribute.
	 * @param fileName
	 *            Name of file.
	 * @return Path to file.
	 * @throws URISyntaxException
	 *             If the codebase argument is an invalid URI syntax.
	 */
	public static Path getPathToLaunchConfigurationFile(String codeBase, String fileName) throws URISyntaxException {

		// get file path
		Path cfgFile = getPathToFile(codeBase, fileName);

		// cannot find executable (remove all white spaces from app name)
		if (!cfgFile.toFile().exists()) {
			Path parent = cfgFile.getParent();
			if (parent != null && parent.toFile().exists()) {
				cfgFile = parent.resolve(cfgFile.getFileName().toString().replaceAll("\\s+", ""));
			}
		}

		// return CFG file
		return cfgFile;
	}

	/**
	 * Returns the path to file. Note that, the file should be located directly in application codebase.
	 *
	 * @param codeBase
	 *            Code base attribute.
	 * @param fileName
	 *            Name of file.
	 * @return Path to file.
	 * @throws URISyntaxException
	 *             If the codebase argument is an invalid URI syntax.
	 */
	public static Path getPathToFile(String codeBase, String fileName) throws URISyntaxException {

		// null or empty codebase string
		if (codeBase == null || codeBase.isEmpty())
			return Paths.get(fileName);

		// codebase attribute exists
		return Paths.get(new URI(codeBase)).resolve(fileName);
	}

	/**
	 * Finds and returns the path to application jar file, or null if no jar file could be found.
	 *
	 * @param appDir
	 *            Application directory.
	 * @return The path to application jar file, or null if no jar file could be found.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static Path getPathToAppJar(Path appDir) throws IOException {
		try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(appDir, getFileFilter(".jar"))) {
			Iterator<Path> iterator = dirStream.iterator();
			while (iterator.hasNext())
				return iterator.next();
		}
		return null;
	}

	/**
	 * Finds and returns the path to application manifest file, or null if no manifest file could be found.
	 *
	 * @param appDir
	 *            Application directory.
	 * @return The path to application manifest file, or null if no manifest file could be found.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static Path getPathToAppManifest(Path appDir) throws IOException {
		try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(appDir, getFileFilter(".MF"))) {
			Iterator<Path> iterator = dirStream.iterator();
			while (iterator.hasNext())
				return iterator.next();
		}
		return null;
	}

	/**
	 * Creates and returns a new logger.
	 *
	 * @param level
	 *            Log level.
	 * @return The newly created logger.
	 */
	public static Logger createLogger(Level level) {

		try {

			// create logger
			Logger logger = Logger.getLogger(AppContainer.class.getName());

			// create file handler
			FileHandler fileHandler = new FileHandler(AppContainer.LOG_FILE.toString());

			// set simple formatter to file handler
			fileHandler.setFormatter(new SimpleFormatter());

			// add handler to logger
			logger.addHandler(fileHandler);

			// set log level (info, warning and severe are logged)
			logger.setLevel(level);

			// return logger
			return logger;
		}

		// exception occurred during creating logger
		catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	/**
	 * Returns file filter for the given file types.
	 *
	 * @param extensions
	 *            File name extensions.
	 * @return File filter.
	 */
	public static DirectoryStream.Filter<Path> getFileFilter(String... extensions) {

		// create file filter
		DirectoryStream.Filter<Path> filter = file -> {
			Path fileNamePath = file.getFileName();
			if (fileNamePath == null)
				return false;
			String fileName = fileNamePath.toString().toLowerCase();
			for (String extension : extensions)
				if (fileName.endsWith(extension.toLowerCase()))
					return true;
			return false;
		};

		// return filter
		return filter;
	}

	/**
	 * Shuts down the given thread executor in two phases, first by calling shutdown to reject incoming tasks, and then calling shutdownNow, if necessary, to cancel any lingering tasks.
	 *
	 * @param executor
	 *            Thread executor to shutdown.
	 */
	public static void shutdownThreadExecutor(ExecutorService executor) {

		// disable new tasks from being submitted
		executor.shutdown();

		try {

			// wait a while for existing tasks to terminate
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {

				// cancel currently executing tasks
				executor.shutdownNow();

				// wait a while for tasks to respond to being canceled
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					AppContainer.LOGGER.warning("Thread pool " + executor.toString() + " did not terminate.");
				}
			}
		}

		// exception occurred during shutting down the thread pool
		catch (InterruptedException ie) {

			// cancel if current thread also interrupted
			executor.shutdownNow();

			// preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Creates and shows exception dialog.
	 *
	 * @param header
	 *            Header of dialog.
	 * @param message
	 *            Message.
	 * @param e
	 *            Exception.
	 */
	public static void showExceptionDialog(String header, String message, Throwable e) {

		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Problem Encountered");
		alert.setHeaderText(header);
		alert.setContentText(message);

		// Create expandable Exception.
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String exceptionText = sw.toString();

		Label label = new Label("Exception stacktrace:");

		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(false);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);

		// Set expandable Exception into the dialog pane.
		alert.getDialogPane().setExpandableContent(expContent);

		// show
		alert.showAndWait();
	}

	/**
	 * Returns the application resource versions from the jar file which are defined in the application settings, or null if resource versions couldn't be found.
	 *
	 * @param jarFile
	 *            Path to jar file.
	 * @param settings
	 *            Application settings.
	 * @return The application resource versions from the jar file which are defined in the application settings, or null if resource versions couldn't be found.
	 * @throws Exception
	 *             If exception occurs during processing the jar file.
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, String> getResourceVersionsFromJar(Path jarFile, Settings settings) throws Exception {

		// null jar file, doesn't exist or not a regular file
		if (jarFile == null || !Files.exists(jarFile) || !Files.isRegularFile(jarFile))
			return null;

		// get application resources from settings
		ArrayList<ApplicationResource> resources = (ArrayList<ApplicationResource>) settings.getSetting(Settings.APP_RESOURCES);
		if (resources == null || resources.isEmpty())
			return null;

		// create mapping to store manifest attributes
		HashMap<String, String> manifestAttributes = new HashMap<>();

		// load jar file
		try (JarFile jarfile = new JarFile(jarFile.toFile())) {

			// get manifest
			Manifest manifest = jarfile.getManifest();

			// get manifest attributes
			Attributes attributes = manifest.getMainAttributes();

			// loop over application resources
			for (ApplicationResource resource : resources) {
				String attributeName = resource.getManifestAttribute();
				if (attributeName == null) {
					continue;
				}
				String attributeValue = attributes.getValue(attributeName);
				if (attributeValue == null) {
					continue;
				}
				manifestAttributes.put(attributeName, attributeValue);
			}
		}

		// return manifest attributes
		return manifestAttributes.isEmpty() ? null : manifestAttributes;
	}

	/**
	 * Returns the application resource versions from the manifest file which are defined in the application settings, or null if resource versions couldn't be found.
	 *
	 * @param manifestFile
	 *            Path to manifest file.
	 * @param settings
	 *            Application settings.
	 * @return The application resource versions from the manifest file which are defined in the application settings, or null if resource versions couldn't be found.
	 * @throws Exception
	 *             If exception occurs during processing the manifest file.
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, String> getResourceVersionsFromManifest(Path manifestFile, Settings settings) throws Exception {

		// null manifest file, doesn't exist or not a regular file
		if (manifestFile == null || !Files.exists(manifestFile) || !Files.isRegularFile(manifestFile))
			return null;

		// get application resources from settings
		ArrayList<ApplicationResource> resources = (ArrayList<ApplicationResource>) settings.getSetting(Settings.APP_RESOURCES);
		if (resources == null || resources.isEmpty())
			return null;

		// create mapping to store manifest attributes
		HashMap<String, String> manifestAttributes = new HashMap<>();

		// load manifest file
		try (InputStream inputStream = Files.newInputStream(manifestFile)) {

			// get manifest
			Manifest manifest = new Manifest(inputStream);

			// get manifest attributes
			Attributes attributes = manifest.getMainAttributes();

			// loop over application resources
			for (ApplicationResource resource : resources) {
				String attributeName = resource.getManifestAttribute();
				if (attributeName == null) {
					continue;
				}
				String attributeValue = attributes.getValue(attributeName);
				if (attributeValue == null) {
					continue;
				}
				manifestAttributes.put(attributeName, attributeValue);
			}
		}

		// return manifest attributes
		return manifestAttributes.isEmpty() ? null : manifestAttributes;
	}

	/**
	 * Returns the JAR file manifest attribute value for the given name, or null if attribute value couldn't be found.
	 *
	 * @param jarFile
	 *            Path to jar file.
	 * @param attributeName
	 *            Attribute name.
	 * @return The JAR file manifest attribute value for the given name, or null if attribute value couldn't be found.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static String getManifestAttributeValueFromJar(Path jarFile, String attributeName) throws IOException {
		if (jarFile != null && Files.exists(jarFile) && Files.isRegularFile(jarFile)) {
			try (JarFile jarfile = new JarFile(jarFile.toFile())) {
				Manifest manifest = jarfile.getManifest();
				Attributes attributes = manifest.getMainAttributes();
				return attributes.getValue(attributeName);
			}
		}
		return null;
	}

	/**
	 * Returns the manifest attribute value for the given name, or null if attribute value couldn't be found.
	 *
	 * @param manifestFile
	 *            Path to manifest file.
	 * @param attributeName
	 *            Attribute name.
	 * @return The manifest attribute value for the given name, or null if attribute value couldn't be found.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static String getManifestAttributeValueFromManifest(Path manifestFile, String attributeName) throws IOException {
		if (manifestFile != null && Files.exists(manifestFile) && Files.isRegularFile(manifestFile)) {
			try (InputStream inputStream = Files.newInputStream(manifestFile)) {
				Manifest manifest = new Manifest(inputStream);
				Attributes attributes = manifest.getMainAttributes();
				return attributes.getValue(attributeName);
			}
		}
		return null;
	}

	/**
	 * Builds and returns connection to filer SFTP server. Note that, the supplied session, channel and sftpChannel objects must be disconnected after usage.
	 *
	 * @param settings
	 *            App Launcher settings.
	 * @return SFTP server connection.
	 * @throws JSchException
	 *             If filer connection cannot be established.
	 */
	public static SFTPConnection createSFTPConnection(Settings settings) throws JSchException {

		// set connection properties
		String username = (String) settings.getSetting(Settings.SFTP_USERNAME);
		String hostname = (String) settings.getSetting(Settings.SFTP_HOSTNAME);
		int port = Integer.parseInt((String) settings.getSetting(Settings.SFTP_PORT));
		String password = (String) settings.getSetting(Settings.SFTP_PASSWORD);
		int timeout = Integer.parseInt((String) settings.getSetting(Settings.CONNECTION_TIMEOUT));

		// create session
		JSch jsch = new JSch();
		Session session = jsch.getSession(username, hostname, port);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
		session.setPassword(password);
		session.connect(timeout);

		// open channel and connect
		Channel channel = session.openChannel("sftp");
		channel.connect();
		ChannelSftp sftpChannel = (ChannelSftp) channel;

		// create and return connection object
		return new SFTPConnection(session, channel, sftpChannel, AppContainer.LOGGER);
	}

	/**
	 * Builds and returns connection to filer SFTP server. Note that, the supplied session, channel and sftpChannel objects must be disconnected after usage.
	 *
	 * @param settings
	 *            App Launcher settings.
	 * @throws JSchException
	 *             If filer connection cannot be established.
	 */
	public static void pingSFTPConnection(Settings settings) throws JSchException {

		// initialize session
		Session session = null;

		try {

			// set connection properties
			String username = (String) settings.getSetting(Settings.SFTP_USERNAME);
			String hostname = (String) settings.getSetting(Settings.SFTP_HOSTNAME);
			int port = Integer.parseInt((String) settings.getSetting(Settings.SFTP_PORT));
			String password = (String) settings.getSetting(Settings.SFTP_PASSWORD);
			int timeout = Integer.parseInt((String) settings.getSetting(Settings.CONNECTION_TIMEOUT));

			// create session
			JSch jsch = new JSch();
			session = jsch.getSession(username, hostname, port);
			session.setConfig("StrictHostKeyChecking", "no");
			session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
			session.setPassword(password);
			session.connect(timeout);
		}

		// clean up
		finally {
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	/**
	 * Pings a HTTP URL. This effectively sends a HEAD request and returns <code>true</code> if the response code is in the 200-399 range.
	 *
	 * @param url
	 *            The HTTP URL to be pinged.
	 * @param timeout
	 *            The timeout in millis for both the connection timeout and the response read timeout. Note that the total timeout is effectively two times the given timeout.
	 * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request within the given timeout, otherwise <code>false</code>.
	 */
	public static boolean pingURL(String url, int timeout) {
		url = url.replaceFirst("^https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.

		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(timeout);
			connection.setReadTimeout(timeout);
			connection.setRequestMethod("HEAD");
			int responseCode = connection.getResponseCode();
			return 200 <= responseCode && responseCode <= 399;
		}
		catch (IOException exception) {
			return false;
		}
	}

	/**
	 * Deletes given file recursively.
	 *
	 * @param path
	 *            Path to directory where temporary files are kept.
	 * @param keep
	 *            Files to keep.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static void deleteTemporaryFiles(Path path, Path... keep) throws IOException {

		// null path
		if (path == null)
			return;

		// directory
		if (Files.isDirectory(path)) {

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				while (iterator.hasNext()) {
					deleteTemporaryFiles(iterator.next(), keep);
				}
			}

			// delete directory (if not to be kept)
			try {
				if (!containsFile(path, keep)) {
					Files.delete(path);
				}
			}

			// directory not empty
			catch (DirectoryNotEmptyException e) {
				// ignore
			}
		}
		else if (!containsFile(path, keep)) {
			Files.delete(path);
		}
	}

	/**
	 * Returns true if given target file is contained within the given files array.
	 *
	 * @param target
	 *            Target file to search for.
	 * @param files
	 *            Array of files to search the target file.
	 * @return True if given target file is contained within the given files array.
	 */
	public static boolean containsFile(Path target, Path... files) {
		for (Path file : files)
			if (file.equals(target))
				return true;
		return false;
	}

	/**
	 * Downloads file from web server.
	 *
	 * @param targetPath
	 *            Local path to target file.
	 * @param sourceURL
	 *            URL of the source file in the webserver.
	 * @param listener
	 *            Download listener.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void download(String targetPath, URL sourceURL, DownloadListener listener) throws Exception {
		try (ReadableByteChannel rbc = new RBCWrapper(Channels.newChannel(sourceURL.openStream()), getURLContentLength(sourceURL), listener)) {
			try (FileOutputStream fos = new FileOutputStream(targetPath)) {
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			}
		}
	}

	/**
	 * Restarts App Container.
	 *
	 * @param appName
	 *            Application name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void restartContainer(String appName) throws Exception {

		// get path to installation directory
		Path installationDirectory = getPathToInstallationDirectory(AppContainer.OS_TYPE);

		// macOSX
		if (AppContainer.OS_TYPE.equals(MACOS)) {
			executeOnMacOSX(installationDirectory, appName);
		}
		else if (AppContainer.OS_TYPE.equals(WINDOWS)) {
			executeOnWindows(installationDirectory, appName);
		}
		else if (AppContainer.OS_TYPE.equals(LINUX)) {
			executeOnLinux(installationDirectory, appName);
		}
	}

	/**
	 * Returns content length of the given URL.
	 *
	 * @param url
	 *            URL to get the content length.
	 * @return The content length of the given URL.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static int getURLContentLength(URL url) throws Exception {

		// do not follow redirects
		HttpURLConnection.setFollowRedirects(false);

		// connect to URL
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		// set request method
		connection.setRequestMethod("HEAD");

		// return content length
		return connection.getContentLength();
	}

	/**
	 * Returns path to AppLauncher installation directory.
	 *
	 * @param osType
	 *            Operating system type.
	 * @return The AppLauncher installation directory.
	 * @throws Exception
	 *             If installation directory could not be found.
	 */
	private static Path getPathToInstallationDirectory(String osType) throws Exception {

		// initialize path
		Path path = null;

		// windows
		if (osType.equals(WINDOWS)) {
			path = AppContainer.LOG_FILE.getParent(); // app directory
			if (path == null)
				throw new Exception("Null app directory encountered.");
			path = path.getParent(); // AppLauncher directory
		}

		// macOSX
		else if (osType.equals(MACOS)) {
			path = AppContainer.LOG_FILE.getParent(); // Java directory
			if (path == null)
				throw new Exception("Null Java directory encountered.");
			path = path.getParent(); // Contents directory
			if (path == null)
				throw new Exception("Null contents directory encountered.");
			path = path.getParent(); // AppLauncher directory
		}

		// linux
		else if (osType.equals(LINUX)) {
			path = AppContainer.LOG_FILE.getParent(); // app directory
			if (path == null)
				throw new Exception("Null app directory encountered.");
			path = path.getParent(); // AppLauncher directory
		}

		// return path
		return path;
	}

	/**
	 * Executes application startup command for windows operating system.
	 *
	 * @param updaterPath
	 *            Path to updater installation directory.
	 * @param appName
	 *            Application name.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	private static void executeOnWindows(Path updaterPath, String appName) throws IOException {

		// get executable
		Path executable = updaterPath.resolve(appName);

		// cannot find executable (remove all white spaces from app name
		if (!Files.exists(executable)) {
			executable = updaterPath.resolve(appName.replaceAll("\\s+", ""));
		}

		// create process builder
		ProcessBuilder pb = new ProcessBuilder(executable.toAbsolutePath().toString());
		pb.directory(updaterPath.toFile());

		// execute
		pb.start();
	}

	/**
	 * Executes application startup command for macOSX operating system.
	 *
	 * @param updaterPath
	 *            Path to updater installation directory.
	 * @param appName
	 *            Application name.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	private static void executeOnMacOSX(Path updaterPath, String appName) throws IOException {

		// create execution command and working directory path
		Path workingDir = updaterPath.resolve("Contents").resolve("MacOS"); // MacOSX directory

		// get executable
		Path executable = workingDir.resolve(appName);

		// cannot find executable (remove all white spaces from app name
		if (!Files.exists(executable)) {
			executable = workingDir.resolve(appName.replaceAll("\\s+", ""));
		}

		// create process builder
		ProcessBuilder pb = new ProcessBuilder(executable.toAbsolutePath().toString());
		pb.directory(workingDir.toFile());

		// execute
		pb.start();
	}

	/**
	 * Executes application startup command for linux operating system.
	 *
	 * @param updaterPath
	 *            Path to updater installation directory.
	 * @param appName
	 *            Application name.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	private static void executeOnLinux(Path updaterPath, String appName) throws IOException {

		// get executable
		Path executable = updaterPath.resolve(appName);

		// cannot find executable (remove all white spaces from app name
		if (!Files.exists(executable)) {
			executable = updaterPath.resolve(appName.replaceAll("\\s+", ""));
		}

		// create process builder
		ProcessBuilder pb = new ProcessBuilder(executable.toAbsolutePath().toString());
		pb.directory(updaterPath.toFile());

		// execute
		pb.start();
	}
}
