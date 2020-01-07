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
import java.util.Arrays;
import java.util.ResourceBundle;

import container.AppContainer;
import container.data.Settings;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Pagination;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/**
 * Class for settings panel controller.
 *
 * @author Murat Artim
 * @date 13 May 2018
 * @time 11:29:17
 */
public class SettingsPanel implements Initializable {

	/** The owner dialog header. */
	private SettingsHeader header;

	/** Sub panels. */
	private HostingTypePanel[] subPanels = new HostingTypePanel[2];

	@FXML
	private VBox root;

	@FXML
	private ToggleGroup hostingType;

	@FXML
	private Pagination pagination;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup pagination page factory
		pagination.setPageFactory(pageIndex -> subPanels[pageIndex].getRoot());

		// add listener to hosting type changes
		hostingType.selectedToggleProperty().addListener((ChangeListener<Toggle>) (observable, oldValue, newValue) -> {

			// toggle unselected
			if (newValue == null) {
				hostingType.selectToggle(oldValue);
				return;
			}

			// get selected toggle text
			String text = ((ToggleButton) newValue).getText();

			// set header
			header.setupHeader(text);

			// show sub panel
			pagination.setCurrentPageIndex(text.equals(Settings.WEB_HOSTING) ? 0 : 1);
		});
	}

	/**
	 * Returns the root pane of the settings panel.
	 *
	 * @return The root pane of the settings panel.
	 */
	public VBox getRoot() {
		return root;
	}

	/**
	 * Creates and returns a new settings object based on component entries.
	 *
	 * @return New settings object.
	 */
	public Settings createSettings() {
		return subPanels[pagination.getCurrentPageIndex()].createSettings();
	}

	/**
	 * Sets panel components based on given settings.
	 *
	 * @param settings
	 *            App launcher settings.
	 */
	public void setFromSettings(Settings settings) {

		// setup sub panels
		Arrays.stream(subPanels).forEach(x -> x.setFromSettings(settings));

		// select hosting type
		hostingType.getToggles().forEach(x -> {
			String text = ((ToggleButton) x).getText();
			if (text.equals(settings.getSetting(Settings.HOSTING_TYPE))) {
				hostingType.selectToggle(x);
			}
		});
	}

	/**
	 * Loads and returns the main panel of launcher.
	 *
	 * @param settings
	 *            App launcher settings.
	 * @param header
	 *            The owner dialog header.
	 * @return The newly loaded main panel.
	 */
	public static SettingsPanel load(Settings settings, SettingsHeader header) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(AppContainer.class.getResource("fxml/SettingsPanel.fxml"));
			fxmlLoader.load();

			// get controller
			SettingsPanel controller = (SettingsPanel) fxmlLoader.getController();

			// set components
			controller.header = header;

			// load sub panels
			controller.subPanels[0] = WebHostingSettingsPanel.load(settings);
			controller.subPanels[1] = SFTPHostingSettingsPanel.load(settings);

			// select hosting type
			controller.hostingType.getToggles().forEach(x -> {
				String text = ((ToggleButton) x).getText();
				if (text.equals(settings.getSetting(Settings.HOSTING_TYPE))) {
					controller.hostingType.selectToggle(x);
				}
			});

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Interface for hosting type setting panels.
	 *
	 * @author Murat Artim
	 * @date 20 May 2018
	 * @time 00:56:38
	 */
	public interface HostingTypePanel extends Initializable {

		/**
		 * Returns the root pane of this panel.
		 *
		 * @return The root pane of this panel.
		 */
		VBox getRoot();

		/**
		 * Creates and returns a new settings object based on component entries.
		 *
		 * @return New settings object.
		 */
		Settings createSettings();

		/**
		 * Sets panel components based on given settings.
		 *
		 * @param settings
		 *            App launcher settings.
		 */
		void setFromSettings(Settings settings);
	}
}
