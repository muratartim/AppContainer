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
package container.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;

import container.AppContainer;

/**
 * Class for application resource.
 *
 * @author Murat Artim
 * @date 18 May 2018
 * @time 23:48:56
 */
public class ApplicationResource {

	/** Resource attribute. */
	private String path, manifestAttribute;

	/** Resource file names. */
	private ArrayList<String> fileNames;

	/**
	 * Returns the path to the resource.
	 *
	 * @return The path to the resource.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns the manifest attribute name of the resource.
	 *
	 * @return The manifest attribute name of the resource.
	 */
	public String getManifestAttribute() {
		return manifestAttribute;
	}

	/**
	 * Returns the file names of the resource.
	 *
	 * @return The file names of the resource.
	 */
	public ArrayList<String> getFileNames() {
		return fileNames;
	}

	/**
	 * Sets the path to the resource.
	 *
	 * @param path
	 *            The path to the resource.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Sets the manifest attribute name of the resource.
	 *
	 * @param manifestAttribute
	 *            The manifest attribute name of the resource.
	 */
	public void setManifestAttribute(String manifestAttribute) {
		this.manifestAttribute = manifestAttribute;
	}

	/**
	 * Sets file names of the resource.
	 *
	 * @param fileNames
	 *            File names to set.
	 */
	public void setFileNames(ArrayList<String> fileNames) {
		this.fileNames = fileNames;
	}

	@Override
	public String toString() {

		// build and return path
		try {
			return path == null ? null : Paths.get(new URI(path).getPath()).getFileName().toString();
		}

		// exception occurred
		catch (URISyntaxException e) {
			AppContainer.LOGGER.log(Level.WARNING, "Exception occurred during building building path to application resource.", e);
			return null;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (fileNames == null ? 0 : fileNames.hashCode());
		result = prime * result + (manifestAttribute == null ? 0 : manifestAttribute.hashCode());
		result = prime * result + (path == null ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApplicationResource other = (ApplicationResource) obj;
		if (fileNames == null) {
			if (other.fileNames != null)
				return false;
		}
		else if (!fileNames.equals(other.fileNames))
			return false;
		if (manifestAttribute == null) {
			if (other.manifestAttribute != null)
				return false;
		}
		else if (!manifestAttribute.equals(other.manifestAttribute))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		}
		else if (!path.equals(other.path))
			return false;
		return true;
	}
}
