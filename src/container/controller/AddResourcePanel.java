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
import container.data.ApplicationResource;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for add resource panel controller.
 *
 * @author Murat Artim
 * @date 20 May 2018
 * @time 01:13:44
 */
public class AddResourcePanel implements Initializable {

	/** Application resource. */
	private ApplicationResource resource;

	/** Index of resource in the containing list. */
	private int index;

	@FXML
	private VBox root;

	@FXML
	private TextField path, manifestAttribute;

	@FXML
	private TextArea fileNames;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
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
	 * Applies changes to this dialog.
	 *
	 * @param resourceList
	 *            Resource list.
	 * @return True if the dialog should be closed.
	 */
	public boolean applyChanges(ListView<ApplicationResource> resourceList) {

		// get inputs
		String pathText = path.getText();
		String manifestAttributeText = manifestAttribute.getText();
		String fileNamesText = fileNames.getText();

		// invalid inputs
		if (pathText == null || pathText.trim().isEmpty()) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Missing Input");
			alert.setHeaderText(null);
			alert.setContentText("Please supply resource path to proceed.");
			alert.showAndWait();
			return false;
		}
		if (manifestAttributeText == null || manifestAttributeText.trim().isEmpty()) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Missing Input");
			alert.setHeaderText(null);
			alert.setContentText("Please supply manifest attribute to proceed.");
			alert.showAndWait();
			return false;
		}
		if (fileNamesText == null || fileNamesText.trim().isEmpty()) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Missing Input");
			alert.setHeaderText(null);
			alert.setContentText("Please supply file name(s) to proceed.");
			alert.showAndWait();
			return false;
		}
		ArrayList<String> resourceFileNames = extractFileNames(fileNamesText);
		if (resourceFileNames == null || resourceFileNames.isEmpty()) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Missing Input");
			alert.setHeaderText(null);
			alert.setContentText("Please supply file name(s) to proceed.");
			alert.showAndWait();
			return false;
		}

		// add new resource
		if (resource == null) {

			// create resource
			resource = new ApplicationResource();
			resource.setPath(pathText);
			resource.setManifestAttribute(manifestAttributeText);
			resource.setFileNames(resourceFileNames);

			// resource already exists
			if (resourceList.getItems().contains(resource)) {
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("Duplicate Entry");
				alert.setHeaderText(null);
				alert.setContentText("Resource already exists. Please supply unique resources.");
				alert.showAndWait();
				return false;
			}

			// add
			resourceList.getItems().add(resource);
			return true;
		}

		// edit existing resource
		ApplicationResource newResource = new ApplicationResource();
		newResource.setPath(pathText);
		newResource.setManifestAttribute(manifestAttributeText);
		newResource.setFileNames(resourceFileNames);
		int checkIndex = resourceList.getItems().indexOf(newResource);

		// no change
		if (checkIndex == index)
			return true;

		// doesn't exist
		if (checkIndex == -1) {
			resourceList.getItems().set(index, newResource);
			return true;
		}

		// exists
		Alert alert = new Alert(AlertType.WARNING);
		alert.setTitle("Duplicate Entry");
		alert.setHeaderText(null);
		alert.setContentText("Resource already exists. Please supply unique resources.");
		alert.showAndWait();
		return false;
	}

	/**
	 * Extracts file names and returns in a list.
	 *
	 * @param fileNamesText
	 *            File names text.
	 * @return List containing file names.
	 */
	private static ArrayList<String> extractFileNames(String fileNamesText) {

		// create list
		ArrayList<String> resourceFileNames = new ArrayList<>();

		// one file name
		if (!fileNamesText.contains(",")) {
			resourceFileNames.add(fileNamesText.trim());
			return resourceFileNames;
		}

		// split from commas
		String[] split = fileNamesText.split(",");
		for (String name : split) {
			name = name.trim();
			if (name.isEmpty()) {
				continue;
			}
			resourceFileNames.add(name);
		}

		// return file names
		return resourceFileNames;
	}

	/**
	 * Loads and returns the main panel of launcher.
	 *
	 * @param resource
	 *            Application resource. This can be null for adding new resource.
	 * @param index
	 *            Index of resource in the containing list.
	 * @return The newly loaded main panel.
	 */
	public static AddResourcePanel load(ApplicationResource resource, int index) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(AppContainer.class.getResource("fxml/AddResourcePanel.fxml"));
			fxmlLoader.load();

			// get controller
			AddResourcePanel controller = (AddResourcePanel) fxmlLoader.getController();

			// set components
			controller.resource = resource;
			controller.index = index;

			// setup components
			if (resource != null) {
				controller.path.setText(resource.getPath());
				controller.manifestAttribute.setText(resource.getManifestAttribute());
				controller.fileNames.setText(String.join(", ", resource.getFileNames()));
			}

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
