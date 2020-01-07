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
import java.util.ArrayList;
import java.util.ResourceBundle;

import container.AppContainer;
import container.controller.SettingsPanel.HostingTypePanel;
import container.data.ApplicationResource;
import container.data.Settings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Class for web hosting settings panel controller.
 *
 * @author Murat Artim
 * @date 13 May 2018
 * @time 11:25:03
 */
public class WebHostingSettingsPanel implements HostingTypePanel {

	@FXML
	private VBox root;

	@FXML
	private TextField webAppName, webManifestUrl, webVersionDescUrl, webConnectionTimeout, updateNotification, updateIgnorance;

	@FXML
	private ListView<ApplicationResource> resourceList;

	@FXML
	private Button remove, edit;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// bind components
		resourceList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		remove.disableProperty().bind(resourceList.getSelectionModel().selectedItemProperty().isNull());
		edit.disableProperty().bind(resourceList.getSelectionModel().selectedItemProperty().isNull());

		// add double click listener
		resourceList.setOnMouseClicked(click -> {
			if (click.getClickCount() == 2) {
				if (resourceList.getSelectionModel().isEmpty())
					return;
				onEditResourceClicked();
			}
		});
	}

	@Override
	public VBox getRoot() {
		return root;
	}

	@Override
	public Settings createSettings() {
		Settings settings = new Settings();
		settings.put(Settings.HOSTING_TYPE, Settings.WEB_HOSTING);
		settings.put(Settings.APP_NAME, webAppName.getText());
		settings.put(Settings.MANIFEST_LOCATION, webManifestUrl.getText());
		settings.put(Settings.VERSION_DESC_URL, webVersionDescUrl.getText());
		settings.put(Settings.CONNECTION_TIMEOUT, webConnectionTimeout.getText());
		settings.put(Settings.MANIFEST_ATTRIBUTE_FOR_UPDATE_NOTIFICATION, updateNotification.getText());
		settings.put(Settings.MANIFEST_ATTRIBUTE_FOR_IGNORE_UPDATE_ALLOWANCE, updateIgnorance.getText());
		ArrayList<ApplicationResource> resources = new ArrayList<>();
		resources.addAll(resourceList.getItems());
		settings.put(Settings.APP_RESOURCES, resources);
		return settings;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setFromSettings(Settings settings) {

		// get hosting type
		String hostingType = (String) settings.getSetting(Settings.HOSTING_TYPE);

		// not web hosting
		if (hostingType == null || !hostingType.equals(Settings.WEB_HOSTING))
			return;

		// setup components
		webAppName.setText((String) settings.getSetting(Settings.APP_NAME));
		webManifestUrl.setText((String) settings.getSetting(Settings.MANIFEST_LOCATION));
		webVersionDescUrl.setText((String) settings.getSetting(Settings.VERSION_DESC_URL));
		webConnectionTimeout.setText((String) settings.getSetting(Settings.CONNECTION_TIMEOUT));
		updateNotification.setText((String) settings.getSetting(Settings.MANIFEST_ATTRIBUTE_FOR_UPDATE_NOTIFICATION));
		updateIgnorance.setText((String) settings.getSetting(Settings.MANIFEST_ATTRIBUTE_FOR_IGNORE_UPDATE_ALLOWANCE));
		ArrayList<ApplicationResource> resources = (ArrayList<ApplicationResource>) settings.getSetting(Settings.APP_RESOURCES);
		if (resources != null) {
			resourceList.getItems().setAll(resources);
		}
	}

	@FXML
	private void onAddResourceClicked() {
		showAddResourceDialog(true);
	}

	@FXML
	private void onRemoveResourceClicked() {
		ApplicationResource resource = resourceList.getSelectionModel().getSelectedItem();
		if (resource != null) {
			resourceList.getItems().remove(resource);
		}
	}

	@FXML
	private void onEditResourceClicked() {
		showAddResourceDialog(false);
	}

	/**
	 * Shows add/edit resource dialog.
	 *
	 * @param isAdd
	 *            True to add new resource.
	 */
	private void showAddResourceDialog(boolean isAdd) {

		// create dialog
		Dialog<Void> dialog = new Dialog<>();
		dialog.setTitle(isAdd ? "Add New Resource" : "Edit Resource");
		dialog.setResizable(false);
		dialog.getDialogPane().setHeader(null);

		// create and add settings panel
		ApplicationResource resource = isAdd ? null : resourceList.getSelectionModel().getSelectedItem();
		int index = isAdd ? -1 : resourceList.getItems().indexOf(resource);
		AddResourcePanel resourcePanel = AddResourcePanel.load(resource, index);
		dialog.getDialogPane().setContent(resourcePanel.getRoot());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image("container/image/icon.png"));

		// add buttons to dialog
		ButtonType apply = new ButtonType("Apply", ButtonData.APPLY);
		dialog.getDialogPane().getButtonTypes().add(apply);
		ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		dialog.getDialogPane().getButtonTypes().add(cancel);

		// setup reset button action
		final Button applyButton = (Button) dialog.getDialogPane().lookupButton(apply);
		applyButton.addEventFilter(ActionEvent.ACTION, event -> {
			if (!resourcePanel.applyChanges(resourceList)) {
				event.consume();
			}
		});

		// show dialog
		dialog.showAndWait();
	}

	/**
	 * Loads and returns the main panel of launcher.
	 *
	 * @param settings
	 *            App launcher settings.
	 * @return The newly loaded main panel.
	 */
	public static WebHostingSettingsPanel load(Settings settings) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(AppContainer.class.getResource("fxml/WebHostingSettingsPanel.fxml"));
			fxmlLoader.load();

			// get controller
			WebHostingSettingsPanel controller = (WebHostingSettingsPanel) fxmlLoader.getController();

			// set components
			controller.setFromSettings(settings);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
