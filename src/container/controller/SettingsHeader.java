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
package container.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import container.AppContainer;
import container.data.Settings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Class for SFTP hosting header controller.
 *
 * @author Murat Artim
 * @date 12 May 2018
 * @time 01:47:44
 */
public class SettingsHeader implements Initializable {

	@FXML
	private HBox root;

	@FXML
	private ImageView image;

	@FXML
	private Label title, text;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Returns the root element of this panel.
	 *
	 * @return The root element of this panel.
	 */
	public HBox getRoot() {
		return root;
	}

	/**
	 * Sets up the header based on given settings.
	 *
	 * @param hostingType
	 *            Application hosting type.
	 */
	public void setupHeader(String hostingType) {

		// get hosting type
		boolean isWebHosting = hostingType == null ? true : hostingType.equals(Settings.WEB_HOSTING);

		// web hosting
		if (isWebHosting) {
			image.setImage(new Image("container/image/webhosting.png"));
			title.setText("Web Hosting");
			text.setText("Application is hosted and deployed through a web server. App Container will check for and download updates over the internet.");
		}

		// SFTP hosting
		else {
			image.setImage(new Image("container/image/sftphosting.png"));
			title.setText("SFTP Hosting");
			text.setText("Application is hosted and deployed through a SFTP server. App Container will check for and download updates over a filer.");
		}
	}

	/**
	 * Loads and returns the main panel of launcher.
	 *
	 * @param hostingType
	 *            Application hosting type.
	 *
	 * @return The newly loaded main panel.
	 */
	public static SettingsHeader load(String hostingType) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(AppContainer.class.getResource("fxml/SettingsHeader.fxml"));
			fxmlLoader.load();

			// get controller
			SettingsHeader controller = (SettingsHeader) fxmlLoader.getController();
			controller.setupHeader(hostingType);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
