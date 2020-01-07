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
package container.utility;

import java.io.Closeable;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Utility class for SFTP server connection.
 *
 * @author Murat Artim
 * @date 02.11.2017
 * @time 13:10:29
 *
 */
public class SFTPConnection implements Closeable {

	/** Session. */
	private final Session session;

	/** Channel. */
	private final Channel channel;

	/** SFTP channel. */
	private final ChannelSftp sftpChannel;

	/** Logger. */
	private final Logger logger;

	/**
	 * Creates SFTP server connection object.
	 *
	 * @param session
	 *            Session.
	 * @param channel
	 *            Channel.
	 * @param sftpChannel
	 *            SFTP channel.
	 * @param logger
	 *            Logger.
	 */
	public SFTPConnection(Session session, Channel channel, ChannelSftp sftpChannel, Logger logger) {
		this.session = session;
		this.channel = channel;
		this.sftpChannel = sftpChannel;
		this.logger = logger;
		logger.info("SFTP server connection created.");
	}

	/**
	 * Returns session.
	 *
	 * @return Session.
	 */
	public Session getSession() {
		return session;
	}

	/**
	 * Returns channel.
	 *
	 * @return Channel.
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 * Returns SFTP channel.
	 *
	 * @return SFTP channel.
	 */
	public ChannelSftp getSftpChannel() {
		return sftpChannel;
	}

	/**
	 * Disconnects all SFTP server connection objects.
	 */
	public void disconnect() {
		sftpChannel.disconnect();
		logger.info("SFTP channel disconnected.");
		channel.disconnect();
		logger.info("Channel disconnected.");
		session.disconnect();
		logger.info("Session disconnected.");
	}

	/**
	 * Returns true if file or directory at given path exists.
	 *
	 * @param path
	 *            Path to file or directory to check.
	 * @return True if file or directory at given path exists, otherwise false.
	 * @throws SftpException
	 *             If exception occurs during checking file/directory existence.
	 */
	public boolean fileExists(String path) throws SftpException {

		// null path
		if (path == null)
			return false;

		// check if file/directory exists
		try {
			sftpChannel.stat(path);
			return true;
		}

		// exception occurred
		catch (SftpException e) {

			// no such file
			if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
				return false;

			// some other exception occurred (propagate)
			throw e;
		}
	}

	/**
	 * Deletes all files under the given parent directory and with given extension.
	 *
	 * @param directory
	 *            Parent directory.
	 * @param extension
	 *            File extension.Null can be given to delete all files.
	 * @throws SftpException
	 *             If exception occurs during deleting files.
	 */
	public void deleteFilesInDirectory(String directory, String extension) throws SftpException {

		// list all files under directory
		Vector<LsEntry> entries = sftpChannel.ls(directory);

		// loop over entries
		for (LsEntry entry : entries) {

			// get file name
			String fileName = entry.getFilename();

			// hidden or temporary file
			if (fileName.startsWith(".")) {
				continue;
			}

			// set file path
			String filePath = directory + "/" + fileName;

			// file doesn't exist
			if (!fileExists(filePath)) {
				continue;
			}

			// directory
			if (sftpChannel.stat(filePath).isDir()) {
				deleteFilesInDirectory(filePath, extension);
				sftpChannel.rmdir(filePath);
				continue;
			}

			// extension mismatch
			if ((extension != null) && !fileName.toLowerCase().endsWith(extension.toLowerCase())) {
				continue;
			}

			// delete file
			sftpChannel.rm(filePath);
			logger.info(fileName + " deleted.");
		}
	}

	/**
	 * Creates directories.
	 *
	 * @param parentPath
	 *            Path to parent directory.
	 * @param directoryNames
	 *            Names of directories from outer most to deepest.
	 * @return The path to deepest directory.
	 * @throws SftpException
	 *             If exception occurs during process.
	 */
	public String createDirectories(String parentPath, String... directoryNames) throws SftpException {

		// create parent directory if it doesn't exist
		if (!fileExists(parentPath)) {
			sftpChannel.mkdir(parentPath);
		}

		// create directories
		String path = parentPath;
		for (String directoryName : directoryNames) {
			path += "/" + directoryName;
			if (!fileExists(path)) {
				sftpChannel.mkdir(path);
			}
		}

		// return path
		return path;
	}

	@Override
	public void close() throws IOException {
		disconnect();
	}
}
