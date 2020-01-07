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
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;

import container.AppContainer;
import container.data.ApplicationResource;
import container.data.Settings;
import container.task.DeleteAppResources;
import container.task.PingConnection;
import container.task.StartApplication;
import container.utility.Utility;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Class for the main panel of the launcher.
 *
 * @author Murat Artim
 * @date 6 May 2018
 *
 * @time 00:30:09
 */
public class MainPanel implements Initializable {

	/** The owner of this panel. */
	private AppContainer owner;

	/** Application to launch. */
	private Application application;

	/** Currently running task. */
	private Task<?> task;

	/** Exception in case a task fails. */
	private Throwable exception;

	@FXML
	private HBox root;

	@FXML
	private ImageView running, available, failed;

	@FXML
	private Label title, message;

	@FXML
	private Button settings, skip, details, install;

	@FXML
	private ProgressBar progress;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Starts this panel.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public void start() throws Exception {
		startTask(new PingConnection(this));
	}

	/**
	 * Stops this panel.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public void stop() throws Exception {

		// stop launched application (if loaded)
		if (application != null) {
			application.stop();
		}
	}

	/**
	 * Returns the root node of this panel.
	 *
	 * @return The root node.
	 */
	public HBox getRoot() {
		return root;
	}

	/**
	 * Returns the owner of this panel.
	 *
	 * @return The owner of this panel.
	 */
	public AppContainer getOwner() {
		return owner;
	}

	/**
	 * Returns the currently running task of this panel, or null if no task is running at the moment.
	 *
	 * @return The currently running task of this panel, or null if no task is running at the moment.
	 */
	public Task<?> getCurrentTask() {
		return task;
	}

	/**
	 * Gets application to be launched, or null if application could not be loaded.
	 *
	 * @return Application to be launched, or null if application could not be loaded.
	 */
	public Application getApplication() {
		return application;
	}

	/**
	 * Sets application to be launched.
	 *
	 * @param application
	 *            Application to be launched.
	 */
	public void setApplication(Application application) {
		this.application = application;
	}

	/**
	 * Starts given task. It will first unbind the UI from the previous task (if it exists), and bind given task to this panel.
	 *
	 * @param task
	 *            Task to set.
	 */
	public void startTask(Task<?> task) {

		// unbind from previous task
		if (this.task != null) {
			progress.progressProperty().unbind();
			title.textProperty().unbind();
			message.textProperty().unbind();
		}

		// set task
		this.task = task;

		// bind task to this panel
		progress.progressProperty().bind(this.task.progressProperty());
		title.textProperty().bind(this.task.titleProperty());
		message.textProperty().bind(this.task.messageProperty());

		// set notification image
		running.setVisible(true);
		available.setVisible(false);
		failed.setVisible(false);

		// set buttons
		details.setDisable(true);
		install.setDisable(true);
		skip.setVisible(false);

		// start task
		new Thread(this.task).start();
	}

	/**
	 * Called when a task fails.
	 *
	 * @param e
	 *            Exception.
	 * @param actionButtonText
	 *            Action button text.
	 */
	public void taskFailed(Throwable e, String actionButtonText) {

		// set notification image
		running.setVisible(false);
		available.setVisible(false);
		failed.setVisible(true);

		// set buttons
		skip.setVisible(false);
		details.setDisable(false);
		install.setDisable(false);
		install.setText(actionButtonText);

		// store exception
		exception = e;
	}

	/**
	 * Shows error message. Note that this method should not be used from tasks.
	 *
	 * @param titleText
	 *            Title of message.
	 * @param descriptionText
	 *            Description text.
	 * @param e
	 *            Exception message.
	 * @param actionButtonText
	 *            Action button text.
	 */
	public void showError(String titleText, String descriptionText, Throwable e, String actionButtonText) {

		// unbind from previous task
		if (this.task != null) {
			title.textProperty().unbind();
			message.textProperty().unbind();
		}

		// set title and description
		title.setText(titleText);
		message.setText(descriptionText);

		// call failed
		taskFailed(e, actionButtonText);
	}

	/**
	 * Called when update is available.
	 *
	 * @param allowSkipping
	 *            True to allow skipping update.
	 * @param resources
	 *            Application resources to update.
	 */
	public void updateAvailable(boolean allowSkipping, ArrayList<ApplicationResource> resources) {

		// set notification image
		running.setVisible(false);
		available.setVisible(true);
		failed.setVisible(false);

		// set buttons
		skip.setVisible(allowSkipping);
		details.setDisable(false);
		install.setDisable(false);
		install.setText("Install");
		install.setUserData(resources);

		// store exception
		exception = null;
	}

	@FXML
	private void onDetailsClicked() {

		// exception
		if (exception != null) {
			Utility.showExceptionDialog(title.getText(), message.getText(), exception);
		}

		// version description
		else {

			// open default mail application
			try {

				// desktop is not supported
				if (!Desktop.isDesktopSupported()) {
					String message = "Cannot open default browser. Desktop class is not supported.";
					Utility.showExceptionDialog("Cannot access version description", message, new Exception(message));
					return;
				}

				// get desktop
				Desktop desktop = Desktop.getDesktop();

				// open action is not supported
				if (!desktop.isSupported(Desktop.Action.BROWSE)) {
					String message = "Cannot open default browser. Browse action is not supported.";
					Utility.showExceptionDialog("Cannot access version description", message, new Exception(message));
					return;
				}

				// open browser
				URI uri = new URI((String) owner.getSettings().getSetting(Settings.VERSION_DESC_URL));
				desktop.browse(uri);
			}

			// exception occurred
			catch (Exception e) {
				String msg = "Exception occurred during opening default browser: ";
				AppContainer.LOGGER.log(Level.WARNING, msg, e);
				msg += e.getLocalizedMessage();
				msg += " Click 'Details' for more information.";
				Utility.showExceptionDialog("Cannot access version description", msg, e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@FXML
	private void onInstallClicked() {

		// close
		if (install.getText().equals("Close")) {
			Platform.exit();
		}

		// start application
		else if (install.getText().equals("Skip Update")) {
			startTask(new StartApplication(this));
		}

		// install
		else {
			startTask(new DeleteAppResources(this, (ArrayList<ApplicationResource>) install.getUserData()));
		}
	}

	@FXML
	private void onSkipClicked() {
		startTask(new StartApplication(this));
	}

	@FXML
	private void onSettingsClicked() {
		Utility.showSettingsDialog(this);
	}

	@FXML
	private void onMinimizeClicked() {
		owner.getStage().setIconified(true);
	}

	@SuppressWarnings("static-method")
	@FXML
	private void onCreditsClicked() {

		// create dialog
		Dialog<Void> dialog = new Dialog<>();
		dialog.setTitle("Credits");
		dialog.setResizable(false);
		dialog.getDialogPane().setHeader(null);

		// create dialog content
		dialog.getDialogPane().setContent(AboutPanel.load().getRoot());
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image("container/image/icon.png"));

		// add buttons to dialog
		ButtonType ok = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().add(ok);

		// show dialog
		dialog.showAndWait();
	}

	/**
	 * Loads and returns the main panel of launcher.
	 *
	 * @param owner
	 *            The owner application of this panel.
	 *
	 * @return The newly loaded main panel.
	 */
	public static MainPanel load(AppContainer owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(AppContainer.class.getResource("fxml/MainPanel.fxml"));
			fxmlLoader.load();

			// get controller
			MainPanel controller = (MainPanel) fxmlLoader.getController();
			controller.owner = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
