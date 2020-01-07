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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import container.AppContainer;
import container.controller.MainPanel;
import javafx.concurrent.Task;

/**
 * Class for extract application resources task.
 *
 * @author Murat Artim
 * @date 6 May 2018
 * @time 22:51:46
 */
public class ExtractAppResources extends Task<Void> {

	/** Buffer size for extracting zipped files. */
	private static final int BUFSIZE = 2048;

	/** The owner panel. */
	private final MainPanel owner;

	/** Paths to application resources. */
	private final ArrayList<Path> appResources;

	/**
	 * Creates extract application resources task.
	 *
	 * @param owner
	 *            The owner panel.
	 * @param appResources
	 *            Paths to application resources.
	 */
	public ExtractAppResources(MainPanel owner, ArrayList<Path> appResources) {
		this.owner = owner;
		this.appResources = appResources;
	}

	@Override
	protected Void call() throws Exception {

		// update info
		updateTitle("Extracting Application Resources");

		// loop over application resources
		for (Path resource : appResources) {

			// zip archive
			if (resource.getFileName().toString().toLowerCase().endsWith(".zip")) {
				extractAllFilesFromZIP(resource, AppContainer.APP_DIR);
			}

			// not zip archive
			else {
				Files.copy(resource, AppContainer.APP_DIR.resolve(resource.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
			}
		}

		// copy manifest file
		Files.copy(AppContainer.TEMP_DIR.resolve("MANIFEST.MF"), AppContainer.APP_DIR.resolve("MANIFEST.MF"), StandardCopyOption.REPLACE_EXISTING);
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();
		updateProgress(0, 100);

		// update info
		updateMessage("Task completed.");

		// start load application task
		owner.startTask(new StartApplication(owner));
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
	 * Extracts and returns all files from the given ZIP file.
	 *
	 * @param zipPath
	 *            Path to ZIP file.
	 * @param outputDir
	 *            Output directory.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public void extractAllFilesFromZIP(Path zipPath, Path outputDir) throws IOException {

		// open zip file
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {

			// get number of entries
			int numEntries = zipFile.size();

			// get entries
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			// loop over zip entries
			int entryCount = 0;
			while (entries.hasMoreElements()) {

				// get entry
				ZipEntry ze = entries.nextElement();

				// progress info
				updateProgress(entryCount, numEntries);
				entryCount++;

				// not directory
				if (!ze.isDirectory()) {

					// progress info
					updateMessage(ze.getName());

					// create temporary output file
					Path file = outputDir.resolve(ze.getName());

					// create all necessary directories
					Path fileParentDir = file.getParent();
					if (fileParentDir != null) {
						Files.createDirectories(fileParentDir);
					}

					// create output stream
					try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toString()))) {

						// create new buffer
						byte[] buffer = new byte[BUFSIZE];

						// open zip input stream
						try (InputStream zis = zipFile.getInputStream(ze)) {

							// write to output stream
							int len;
							while ((len = zis.read(buffer, 0, BUFSIZE)) != -1) {
								bos.write(buffer, 0, len);
							}
						}
					}

					// file is directory, doesn't exist or hidden
					if (!Files.exists(file) || Files.isDirectory(file) || Files.isHidden(file) || !Files.isRegularFile(file)) {
						continue;
					}
				}
			}
		}
	}
}
