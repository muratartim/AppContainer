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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;

import container.AppContainer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;

/**
 * Class for about panel controller.
 *
 * @author Murat Artim
 * @date 23 May 2018
 * @time 12:06:45
 */
public class AboutPanel implements Initializable {

	@FXML
	private HBox root;

	@FXML
	private Hyperlink name, email;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Returns the root pane of this panel.
	 *
	 * @return The root pane of this panel.
	 */
	public HBox getRoot() {
		return root;
	}

	@SuppressWarnings("static-method")
	@FXML
	private void onNameClicked() {

		// open default mail application
		try {

			// desktop is not supported
			if (!Desktop.isDesktopSupported()) {
				AppContainer.LOGGER.warning("Cannot open default browser. Desktop class is not supported.");
				return;
			}

			// get desktop
			Desktop desktop = Desktop.getDesktop();

			// open action is not supported
			if (!desktop.isSupported(Desktop.Action.BROWSE)) {
				AppContainer.LOGGER.warning("Cannot open default browser. Browse action is not supported.");
				return;
			}

			// open browser
			URI uri = new URI("https://www.linkedin.com/in/muratartim");
			desktop.browse(uri);
		}

		// exception occurred
		catch (Exception e) {
			AppContainer.LOGGER.log(Level.WARNING, "Exception occurred during opening default browser: ", e);
		}
	}

	@FXML
	private void onMailClicked() {

		// open default mail application
		try {

			// desktop is not supported
			if (!Desktop.isDesktopSupported()) {
				AppContainer.LOGGER.warning("Cannot open default mail application. Desktop class is not supported.");
				return;
			}

			// get desktop
			Desktop desktop = Desktop.getDesktop();

			// open action is not supported
			if (!desktop.isSupported(Desktop.Action.MAIL)) {
				AppContainer.LOGGER.warning("Cannot open default mail application. Mail action is not supported.");
				return;
			}

			// open main application
			String developerEmail = email.getText();
			String subject = "Question regarding App Container";
			subject = subject.replaceAll(" ", "%20");
			desktop.mail(new URI("mailto:" + developerEmail + "?subject=" + subject));
		}

		// exception occurred
		catch (Exception e) {
			AppContainer.LOGGER.log(Level.WARNING, "Exception occurred during mailing application credits owner: ", e);
		}
	}

	/**
	 * Loads and returns the root component of this panel.
	 *
	 * @return The newly loaded about panel.
	 */
	public static AboutPanel load() {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(AppContainer.class.getResource("fxml/AboutPanel.fxml"));
			fxmlLoader.load();

			// return controller
			return (AboutPanel) fxmlLoader.getController();
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
