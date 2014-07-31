package co.codewizards.cloudstore.client;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.util.Collection;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.local.persistence.CopyModificationDao;
import co.codewizards.cloudstore.local.persistence.DeleteModificationDao;
import co.codewizards.cloudstore.local.persistence.DirectoryDao;
import co.codewizards.cloudstore.local.persistence.NormalFileDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequestDao;

/**
 * {@link SubCommand} implementation for showing information about a repository in the local file system.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RepoInfoSubCommand extends SubCommandWithExistingLocalRepo
{
	public RepoInfoSubCommand() { }

	protected RepoInfoSubCommand(File localRoot) {
		this.localRoot = assertNotNull("localRoot", localRoot);
		this.localFile = this.localRoot;
		this.local = localRoot.getPath();
	}

	@Override
	public String getSubCommandDescription() {
		return "Show information about an existing repository.";
	}

	@Override
	public void run() throws Exception {
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();
			try {
				showMainProperties(transaction);
//				showRepositoryAliases(transaction);
				showRemoteRepositories(transaction);
				showRemoteRepositoryRequests(transaction);
				showRepositoryStats(transaction);

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			localRepoManager.close();
		}
	}

	private void showMainProperties(LocalRepoTransaction transaction) {
		LocalRepoManager localRepoManager = transaction.getLocalRepoManager();
		Collection<String> repositoryAliases = LocalRepoRegistry.getInstance().getRepositoryAliasesOrFail(localRepoManager.getRepositoryId().toString());
		System.out.println("Local repository:");
		System.out.println("  repository.repositoryId = " + localRepoManager.getRepositoryId());
		System.out.println("  repository.localRoot = " + localRepoManager.getLocalRoot());
		System.out.println("  repository.aliases = " + repositoryAliases);
		System.out.println("  repository.publicKeySha1 = " + HashUtil.sha1ForHuman(localRepoManager.getPublicKey()));
		System.out.println();
	}

//	private void showRepositoryAliases(LocalRepoTransaction transaction) {
//		LocalRepoManager localRepoManager = transaction.getLocalRepoManager();
//		Collection<String> repositoryAliases = LocalRepoRegistry.getInstance().getRepositoryAliasesOrFail(localRepoManager.getRepositoryId().toString());
//		if (repositoryAliases.isEmpty())
//			System.out.println("Aliases: {NONE}");
//		else {
//			System.out.println("Aliases:");
//			for (String repositoryAlias : repositoryAliases)
//				System.out.println("  * " + repositoryAlias);
//		}
//		System.out.println();
//	}

	private void showRemoteRepositories(LocalRepoTransaction transaction) {
		Collection<RemoteRepository> remoteRepositories = transaction.getDao(RemoteRepositoryDao.class).getObjects();
		if (remoteRepositories.isEmpty()) {
			System.out.println("Remote repositories connected: {NONE}");
			System.out.println();
		}
		else {
			System.out.println("Remote repositories connected:");
			for (RemoteRepository remoteRepository : remoteRepositories) {
				System.out.println("  * remoteRepository.repositoryId = " + remoteRepository.getRepositoryId());
				if (remoteRepository.getRemoteRoot() != null)
					System.out.println("    remoteRepository.remoteRoot = " + remoteRepository.getRemoteRoot());

				System.out.println("    remoteRepository.publicKeySha1 = " + HashUtil.sha1ForHuman(remoteRepository.getPublicKey()));
				System.out.println();
			}
		}
	}

	private void showRemoteRepositoryRequests(LocalRepoTransaction transaction) {
		Collection<RemoteRepositoryRequest> remoteRepositoryRequests = transaction.getDao(RemoteRepositoryRequestDao.class).getObjects();
		if (remoteRepositoryRequests.isEmpty()) {
			System.out.println("Remote repositories requesting connection: {NONE}");
			System.out.println();
		}
		else {
			System.out.println("Remote repositories requesting connection:");
			for (RemoteRepositoryRequest remoteRepositoryRequest : remoteRepositoryRequests) {
				System.out.println("  * remoteRepositoryRequest.repositoryId = " + remoteRepositoryRequest.getRepositoryId());
				System.out.println("    remoteRepositoryRequest.publicKeySha1 = " + HashUtil.sha1ForHuman(remoteRepositoryRequest.getPublicKey()));
				System.out.println("    remoteRepositoryRequest.created = " + new DateTime(remoteRepositoryRequest.getCreated()));
				System.out.println("    remoteRepositoryRequest.changed = " + new DateTime(remoteRepositoryRequest.getChanged()));
				System.out.println();
			}
		}
	}

	private void showRepositoryStats(LocalRepoTransaction transaction) {
		NormalFileDao normalFileDao = transaction.getDao(NormalFileDao.class);
		DirectoryDao directoryDao = transaction.getDao(DirectoryDao.class);
		CopyModificationDao copyModificationDao = transaction.getDao(CopyModificationDao.class);
		DeleteModificationDao deleteModificationDao = transaction.getDao(DeleteModificationDao.class);
		long normalFileCount = normalFileDao.getObjectsCount();
		long directoryCount = directoryDao.getObjectsCount();
		long copyModificationCount = copyModificationDao.getObjectsCount();
		long deleteModificationCount = deleteModificationDao.getObjectsCount();

		System.out.println("Statistics:");
		System.out.println("  * Count(NormalFile): " + normalFileCount);
		System.out.println("  * Count(Directory): " + directoryCount);
		System.out.println("  * Count(CopyModification): " + copyModificationCount);
		System.out.println("  * Count(DeleteModification): " + deleteModificationCount);
		System.out.println();
	}
}
