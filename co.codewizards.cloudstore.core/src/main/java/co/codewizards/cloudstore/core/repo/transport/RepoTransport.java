package co.codewizards.cloudstore.core.repo.transport;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * Transport abstraction.
 * <p>
 * The naming in this interface assumes a local client talking to a remote repository. But the
 * repository accessed via this transport does not need to be remote - it might be in the local
 * file system!
 * <p>
 * The synchronisation logic accesses all repositories through this abstraction layer. Therefore,
 * the synchronisation logic does not need to know any details about how to communicate with
 * a repository.
 * <p>
 * There are currently two implementations:
 * <ul>
 * <li>file-system-based (for local repositories)
 * <li>REST-based (for remote repositories)
 * </ul>
 * Further implementations might be written later.
 * <p>
 * An instance of an implementation of {@code RepoTransport} is obtained via the
 * {@link co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory RepoTransportFactory}.
 * <p>
 * <b>Important:</b> Implementors should <i>not</i> directly implement this interface, but instead sub-class
 * {@link AbstractRepoTransport}!
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface RepoTransport {

	/**
	 * Gets the factory which created this instance.
	 * @return the factory which created this instance. Should never be <code>null</code>, if properly initialised.
	 */
	RepoTransportFactory getRepoTransportFactory();
	/**
	 * Sets the factory which created this instance.
	 * @param repoTransportFactory the factory which created this instance. Must not be <code>null</code>.
	 */
	void setRepoTransportFactory(RepoTransportFactory repoTransportFactory);

	/**
	 * Gets the remote repository's root URL, maybe including a {@linkplain #getPathPrefix() path-prefix}.
	 * <p>
	 * This is thus the remote repository's root URL as used to synchronise a certain local repository.
	 * The word "remote" should not be misunderstood as actually on another computer. It just means behind
	 * this transport abstraction.
	 * @return the remote repository's root URL, maybe including a {@linkplain #getPathPrefix() path-prefix}.
	 * Never <code>null</code>, if properly initialised.
	 */
	URL getRemoteRoot();
	void setRemoteRoot(URL remoteRoot);

	UUID getClientRepositoryId();
	void setClientRepositoryId(UUID clientRepositoryId);

	URL getRemoteRootWithoutPathPrefix();
	String getPathPrefix();

	RepositoryDto getRepositoryDto();

	/**
	 * Get the repository's unique ID.
	 * @return the repository's unique ID.
	 */
	UUID getRepositoryId();

	byte[] getPublicKey();

	void close();

	/**
	 * Request to connect this repository with the remote repository identified by the given {@code remoteRepositoryId}.
	 * @param publicKey the public key of the client-repository which requests the connection.
	 */
	void requestRepoConnection(byte[] publicKey);

	ChangeSetDto getChangeSetDto(boolean localSync);

	/**
	 * Creates the specified directory (including all parent-directories).
	 * <p>
	 * If the directory already exists, this is a noop.
	 * <p>
	 * If there is any obstruction in the way of this path (e.g. a normal file), it is moved away (renamed or simply deleted
	 * depending on the conflict resolution strategy).
	 * @param path the path of the directory. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 * @param lastModified the {@linkplain IOUtil#getLastModifiedNoFollow(File) last-modified-timestamp} the newly created
	 * directory will be set to.
	 * May be <code>null</code> (in which case the {@code lastModified} property is not touched). This applies only to the
	 * actual directory and not to the parent-directories! The parent-directories' {@code lastModified} properties are never
	 * touched - even if the parent-directories are newly created.
	 */
	void makeDirectory(String path, Date lastModified);

	void copy(String fromPath, String toPath);
	void move(String fromPath, String toPath);

	/**
	 * Deletes the file (or directory) specified by {@code path}.
	 * <p>
	 * If there is no such file (or directory), this method is a noop.
	 * <p>
	 * If {@code path} denotes a directory, all its children (if there are) are deleted recursively.
	 * @param path the path of the file (or directory) to be deleted. Must not be <code>null</code>. No matter which
	 * operating system is used, the separation-character is always '/'. This path may start with a "/", but there is no
	 * difference, if it does: It is always relative to the repository's root directory.
	 */
	void delete(String path);

	RepoFileDto getRepoFileDto(String path);

	/**
	 * Get the binary file data at the given {@code offset} and with the given {@code length}.
	 * <p>
	 * If the file was modified/deleted, this method should not fail, but simply return <code>null</code>
	 * or a result being shorter than the {@code length} specified.
	 * @param path the path of the file. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 * @param offset the offset of the first byte to be read (0-based).
	 * @param length the length of the data to be read. -1 to read from {@code offset} to the end of the file.
	 */
	byte[] getFileData(String path, long offset, int length);

	/**
	 * Begins a file transfer to this {@code RepoTransport}.
	 * <p>
	 * Usually, this method creates the specified file in the file system (if necessary with parent-directories)
	 * and in the database. But this operation may be deferred until {@link #endPutFile(String, Date, long, String)}.
	 * <p>
	 * If the file is immediately created, it should not be synchronised to any other repository, yet! It should
	 * be ignored, until {@link #endPutFile(String, Date, long, String)} was called for it.
	 * <p>
	 * In normal operation, zero or more invocations of {@link #putFileData(String, long, byte[])} and
	 * finally one invocation of {@link #endPutFile(String, Date, long, String)} follow this method. However, this is not
	 * guaranteed and the file transfer may be interrupted. If it is resumed, later this method is called again,
	 * without {@link #endPutFile(String, Date, long, String)} ever having been called inbetween.
	 * @param path the path of the file. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 * @see #putFileData(String, long, byte[])
	 * @see #endPutFile(String, Date, long, String)
	 */
	void beginPutFile(String path);

	/**
	 * Write a block of binary data into the file.
	 * <p>
	 * This method may only be called after {@link #beginPutFile(String)} and before {@link #endPutFile(String, Date, long, String)}.
	 * @param offset the 0-based position in the file at which the block should be written.
	 * @see #beginPutFile(String)
	 * @see #endPutFile(String, Date, long, String)
	 */
	void putFileData(String path, long offset, byte[] fileData);

	void endPutFile(String path, Date lastModified, long length, String sha1);

	void endSyncFromRepository();

	void endSyncToRepository(long fromLocalRevision);
	String prefixPath(String path);
	String unprefixPath(String path);

	void makeSymlink(String path, String target, Date lastModified);
}
