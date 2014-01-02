package co.codewizards.cloudstore.shared.repo.transport.file;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.AbstractTest;
import co.codewizards.cloudstore.shared.dto.ChangeSetRequest;
import co.codewizards.cloudstore.shared.dto.ChangeSetResponse;
import co.codewizards.cloudstore.shared.dto.DirectoryDTO;
import co.codewizards.cloudstore.shared.dto.RepoFileDTO;
import co.codewizards.cloudstore.shared.dto.RepoFileDTOTreeNode;
import co.codewizards.cloudstore.shared.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.shared.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransport;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactoryRegistry;

public class FileTransportTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(FileTransportTest.class);

	private File remoteRoot;
	private ChangeSetResponse changeSetResponse1;

	@Test
	public void getChangeSetForEntireRepository() throws Exception {
		remoteRoot = newTestRepositoryLocalRoot();
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManager).isNotNull();

		File child_1 = createDirectory(remoteRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		File child_2 = createDirectory(remoteRoot, "2");

		createFileWithRandomContent(child_2, "a");

		File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		File child_3 = createDirectory(remoteRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL);

		ChangeSetRequest changeSetRequest1 = new ChangeSetRequest();
		changeSetRequest1.setRevision(-1);

		changeSetResponse1 = repoTransport.getChangeSet(changeSetRequest1);
		assertThat(changeSetResponse1).isNotNull();
		assertThat(changeSetResponse1.getRepoFileDTOs()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse1.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse1.getRepositoryDTO().getEntityID()).isNotNull();

		// changeSetResponse1 should contain the entire repository - including the root -, because really
		// every localRevision must be > -1.
		assertThat(changeSetResponse1.getRepoFileDTOs()).hasSize(15);

		Set<String> paths = getPaths(changeSetResponse1.getRepoFileDTOs());
		assertThat(paths).containsOnly("/1/a", "/1/b", "/1/c", "/2/a", "/2/1/a", "/2/1/b", "/3/a", "/3/b", "/3/c", "/3/d");

		localRepoManager.close();
	}

	@Test
	public void getChangeSetForAddedFile() throws Exception {
		getChangeSetForEntireRepository();

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		File child_2 = new File(remoteRoot, "2");
		File child_2_1 = new File(child_2, "1");
		createFileWithRandomContent(child_2_1, "c");

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL);

		ChangeSetRequest changeSetRequest2 = new ChangeSetRequest();
		changeSetRequest2.setRevision(changeSetResponse1.getRepositoryDTO().getRevision());

		ChangeSetResponse changeSetResponse2 = repoTransport.getChangeSet(changeSetRequest2);
		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDTOs()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse2.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDTO().getEntityID()).isNotNull();

		// We expect the added file and its direct parent, because they are both modified (new localRevision).
		// Additionally, we expect all parent-directories (recursively) until (including) the root, because they
		// are required to have a complete relative path for each modified RepoFile.
		assertThat(changeSetResponse2.getRepoFileDTOs()).hasSize(4);

		Set<String> paths = getPaths(changeSetResponse2.getRepoFileDTOs());
		assertThat(paths).hasSize(1);
		assertThat(paths.iterator().next()).isEqualTo("/2/1/c");

		localRepoManager.close();
	}

	@Test
	public void getChangeSetForModifiedFile() throws Exception {
		getChangeSetForEntireRepository();

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		File child_2 = new File(remoteRoot, "2");
		File child_2_1 = new File(child_2, "1");
		File child_2_1_b = new File(child_2_1, "b");
		FileOutputStream out = new FileOutputStream(child_2_1_b, true);
		out.write(4);
		out.close();

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL);

		ChangeSetRequest changeSetRequest2 = new ChangeSetRequest();
		changeSetRequest2.setRevision(changeSetResponse1.getRepositoryDTO().getRevision());

		ChangeSetResponse changeSetResponse2 = repoTransport.getChangeSet(changeSetRequest2);
		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDTOs()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse2.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDTO().getEntityID()).isNotNull();

		// We expect the changed file and all parent-directories (recursively) until (including) the
		// root, because they are required to have a complete relative path for each modified RepoFile.
		assertThat(changeSetResponse2.getRepoFileDTOs()).hasSize(4);

		Set<String> paths = getPaths(changeSetResponse2.getRepoFileDTOs());
		assertThat(paths).hasSize(1);
		assertThat(paths.iterator().next()).isEqualTo("/2/1/b");

		localRepoManager.close();
	}

	@Test
	public void getChangeSetForDeletedFile() throws Exception {
		getChangeSetForEntireRepository();

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		File child_2 = new File(remoteRoot, "2");
		File child_2_1 = new File(child_2, "1");
		File child_2_1_b = new File(child_2_1, "b");
		deleteFile(child_2_1_b);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL);

		ChangeSetRequest changeSetRequest2 = new ChangeSetRequest();
		changeSetRequest2.setRevision(changeSetResponse1.getRepositoryDTO().getRevision());

		ChangeSetResponse changeSetResponse2 = repoTransport.getChangeSet(changeSetRequest2);
		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDTOs()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse2.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDTO().getEntityID()).isNotNull();

		// We expect the direct parent of the deleted file, because it is modified (new localRevision).
		// Additionally, we expect all parent-directories (recursively) until (including) the root, because they
		// are required to have a complete relative path for each modified RepoFile.
		assertThat(changeSetResponse2.getRepoFileDTOs()).hasSize(3);

		Set<String> paths = getPaths(changeSetResponse2.getRepoFileDTOs());
		assertThat(paths).hasSize(1);
		assertThat(paths.iterator().next()).isEqualTo("/2/1");

		RepoFileDTOTreeNode rootNode = RepoFileDTOTreeNode.createTree(changeSetResponse2.getRepoFileDTOs());
		List<RepoFileDTOTreeNode> leafs = rootNode.getLeafs();
		RepoFileDTOTreeNode leaf = leafs.get(0);
		assertThat(leaf.getRepoFileDTO()).isInstanceOf(DirectoryDTO.class);
		DirectoryDTO leafDir = (DirectoryDTO) leaf.getRepoFileDTO();
		assertThat(leafDir.isChildNamesLoaded()).isTrue();
		assertThat(leafDir.getChildNames()).containsOnly("a");

		// All parents' childNames should *not* be loaded, because they serve solely the purpose of a complete path
		// and have not been modified.
		RepoFileDTOTreeNode parent = leaf.getParent();
		while (parent != null) {
			assertThat(parent.getRepoFileDTO()).isInstanceOf(DirectoryDTO.class);
			DirectoryDTO parentDirDTO = (DirectoryDTO) parent.getRepoFileDTO();
			assertThat(parentDirDTO.isChildNamesLoaded()).isFalse();
			assertThat(parentDirDTO.getChildNames()).isEmpty();
			parent = parent.getParent();
		}

		localRepoManager.close();
	}

	private Set<String> getPaths(Collection<RepoFileDTO> repoFileDTOs) {
		RepoFileDTOTreeNode rootNode = RepoFileDTOTreeNode.createTree(repoFileDTOs);
		assertThat(rootNode.getRepoFileDTO().getName()).isEqualTo("");
		List<RepoFileDTOTreeNode> leafs = rootNode.getLeafs();
		Set<String> paths = new HashSet<String>(leafs.size());
		for (RepoFileDTOTreeNode leaf : leafs) {
			paths.add(leaf.getPath());
		}
		return paths;
	}

}
