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
package container.task;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

import container.AppContainer;
import container.controller.MainPanel;
import container.data.ApplicationResource;
import container.utility.Utility;
import javafx.concurrent.Task;

/**
 * Class for delete application resources task.
 *
 * @author Murat Artim
 * @date 6 May 2018
 * @time 22:07:48
 */
public class DeleteAppResources extends Task<Void> {

	/** The owner panel. */
	private final MainPanel owner;

	/** Application resources to delete. */
	private final ArrayList<ApplicationResource> resources;

	/**
	 * Creates delete application resources task.
	 *
	 * @param owner
	 *            The owner panel.
	 * @param resources
	 *            Application resources to delete from local application directory.
	 */
	public DeleteAppResources(MainPanel owner, ArrayList<ApplicationResource> resources) {
		this.owner = owner;
		this.resources = resources;
	}

	@Override
	protected Void call() throws Exception {

		// update info
		updateTitle("Deleting Application Resources");

		// loop over resources
		for (ApplicationResource resource : resources) {

			// loop over file names
			for (String fileName : resource.getFileNames()) {

				// delete files recursively
				deleteFiles(AppContainer.APP_DIR.resolve(fileName), AppContainer.APP_DIR);
			}
		}

		// delete manifest file
		deleteFiles(Utility.getPathToAppManifest(AppContainer.APP_DIR), AppContainer.APP_DIR);
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();
		updateProgress(0, 100);

		// update info
		updateMessage("Task completed.");

		// start download app archive task
		owner.startTask(new DownloadAppResources(owner, resources));
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();
		updateProgress(0, 100);

		// update info
		updateMessage("Task failed. Click on 'Details' to see a detailed description of the problem.");

		// notify UI
		owner.taskFailed(getException(), "Close");

		// log exception
		AppContainer.LOGGER.log(Level.SEVERE, getClass().getSimpleName() + " has failed.", getException());
	}

	/**
	 * Deletes given file recursively.
	 *
	 * @param path
	 *            Path to file or directory to delete.
	 * @param keep
	 *            Files to keep.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public void deleteFiles(Path path, Path... keep) throws IOException {

		// null path
		if (path == null || !Files.exists(path))
			return;

		// directory
		if (Files.isDirectory(path)) {

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				while (iterator.hasNext()) {
					deleteFiles(iterator.next(), keep);
				}
			}

			// delete directory (if not to be kept)
			try {
				if (!containsFile(path, keep)) {
					Files.delete(path);
					updateMessage("Deleting directory '" + path.getFileName().toString() + "'");
				}
			}

			// directory not empty
			catch (DirectoryNotEmptyException e) {
				// ignore
			}
		}

		// file
		else {

			// delete file (if not to be kept)
			if (!containsFile(path, keep)) {
				Files.delete(path);
				updateMessage("Deleting file '" + path.getFileName().toString() + "'");
			}
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
}
