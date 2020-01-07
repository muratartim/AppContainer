# AppContainer
[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![HitCount](http://hits.dwyl.io/muratartim/AppContainer.svg)](http://hits.dwyl.io/muratartim/AppContainer)
[![Java Version](https://img.shields.io/badge/java-10-orange.svg)](https://www.oracle.com/technetwork/java/javase/downloads/index.html)

JavaFX application container including automatic update system & runtime. It contains a java rumtime (JRE) and an automatic update system to launch and update embedded applications. It can be deployed via native installers as a full native solution. It supports the hosting of application resources on a web server or SFTP file server.

![whyuseappcontainer](https://user-images.githubusercontent.com/13915745/40880558-9c90a86c-66b3-11e8-9490-05503eae510e.gif)

## Screenshots
<img width="900" alt="screenshot" src="https://user-images.githubusercontent.com/13915745/40854881-1aff635a-65d3-11e8-8556-44101e5f1255.png">

## Video demonstration
Here's a short screencast of the AppContainer in action.

<a href="http://www.youtube.com/watch?feature=player_embedded&v=oj720m8XYNo
" target="_blank"><img src="http://img.youtube.com/vi/oj720m8XYNo/0.jpg" 
alt="JavaFX application container Screencast" width="560" /></a>

## How does it work?
AppContainer wraps JavaFX applications together with java runtime (JRE) and supplies automatic update system. When the user starts the application, AppContainer checks the remote repository for resource updates. In case of available updates, user will be notified and will be given the option to ignore updates (notification and ignoring of updates are optional which can be set with each update). Once the updates are downloaded and extracted, embedded application classes are loaded and the application is started.

## How to use AppContainer?
Here's a step by step description for using AppContainer:

### Prepare your application
- Include the AppContainer jar file [appContainer.jar](https://github.com/muratartim/AppContainer/blob/master/appContainer.jar) in the classpath of your JavaFX project.
- Modify your main application class to extend `container.remote.EmbeddedApplication` instead of `javafx.application.Application`.
- In case exists, replace all references to the method `Application.getHostServices().getCodeBase()` with `EmbeddedApplication.getCodeBase()`.
- Build your JavaFX project to generate the project artifacts (i.e. jar file(s), libraries, external resources etc.)
- Create zip archives of generated project artifacts.
- Modify the manifest of the main jar file to add project artifact versions (see section below).
- Upload archives and the modified manifest file to central repository.

### Manifest file (MANIFEST.MF)
Following attributes should be added to the project manifest file:
```
<Artifact-Name>-Version: 1.0.0
...
Notify-Update: true
Allow-Ignore-Update: true
```
where `<Artifact-Name>` is the name of generated project artifact (all artifacts should be added as separate lines with their version numbers), `Notify-Update` indicates whether the user should be notified of updates before applying them, `Allow-Ignore-Update` indicates whether the user should be allowed to ignore updates.

### Prepare AppContainer
- Generate default AppContainer settings via running the code snippet given in the next section. Note that the code below is an example for SFTP hosted applications. Please refer to AppContainer [javadoc](https://github.com/muratartim/AppContainer/blob/master/javadoc) for web hosting settings.
- Build AppContainer and add the generated default settings file `appContainer.def` to application package.
- Deploy AppContainer (via executable jar file or native installers for target platforms).

### Generating default settings
```java
/**
 * Creates and saves the default settings to 'appContainer.def' file.
 *
 * @param args
 *            Not used.
 */
public static void main(String[] args) {

	// set path to default settings file
	Path defaultSettingsFile = Paths.get("/path/to/appContainer.def");

	// create settings
	Settings settings = new Settings();

	// set standard settings
	settings.put(Settings.HOSTING_TYPE, Settings.SFTP_HOSTING);
	settings.put(Settings.APP_NAME, "Your App Name");
	settings.put(Settings.VERSION_DESC_URL, "http://example.com/path/to/versionDescription.html");
	settings.put(Settings.MANIFEST_LOCATION, "/remote/path/to/MANIFEST.MF");
	settings.put(Settings.CONNECTION_TIMEOUT, "3000");
	settings.put(Settings.SFTP_HOSTNAME, "localhost");
	settings.put(Settings.SFTP_PORT, "22");
	settings.put(Settings.SFTP_USERNAME, "username");
	settings.put(Settings.SFTP_PASSWORD, "password");
	settings.put(Settings.MANIFEST_ATTRIBUTE_FOR_UPDATE_NOTIFICATION, "Notify-Update");
	settings.put(Settings.MANIFEST_ATTRIBUTE_FOR_IGNORE_UPDATE_ALLOWANCE, "Allow-Ignore-Update");

	// set application resources
	ArrayList<ApplicationResource> appResources = new ArrayList<>();
	
	// add artifact1
	ApplicationResource artifact1 = new ApplicationResource();
	artifact1.setPath("/remote/path/to/artifact1.zip");
	artifact1.setManifestAttribute("Artifact1-Version");
	artifact1.setFileNames(new ArrayList<>(Arrays.asList("artifact1.jar")));
	appResources.add(artifact1);
	
	// add artifact2
	ApplicationResource artifact2 = new ApplicationResource();
	artifact2.setPath("/remote/path/to/artifact2.zip");
	artifact2.setManifestAttribute("Artifact2-Version");
	artifact2.setFileNames(new ArrayList<>(Arrays.asList("artifact2.jar")));
	appResources.add(artifact2);
	
	// add more artifacts as needed...
	
	// put application resources to settings
	settings.put(Settings.APP_RESOURCES, appResources);

	// save default settings to file
	settings.saveSettings(defaultSettingsFile);
}
```
