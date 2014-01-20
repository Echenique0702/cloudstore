package co.codewizards.cloudstore.client;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;

public class SyncSubCommand extends SubCommandWithExistingLocalRepo {
	private static final Logger logger = LoggerFactory.getLogger(SyncSubCommand.class);

	@Argument(metaVar="<remote>", index=1, required=false, usage="An ID or URL of a remote repository. If none is specified, all remote repositories are synced.")
	private String remote;

	private EntityID remoteRepositoryID;
	private URL remoteRoot;

	@Option(name="-localOnly", required=false, usage="Synchronise locally only. Do not communicate with any remote repository.")
	private boolean localOnly;

	@Override
	public String getSubCommandName() {
		return "sync";
	}

	@Override
	public String getSubCommandDescription() {
		return "Synchronise a local repository. Depending on the parameters, it synchronises only locally or with one or more remote repositories.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();
		remoteRepositoryID = null;
		remoteRoot = null;
		if (remote != null && !remote.isEmpty()) {
			try {
				remoteRepositoryID = new EntityID(remote);
			} catch (IllegalArgumentException x) {
				try {
					remoteRoot = new URL(remote);
				} catch (MalformedURLException y) {
					throw new IllegalArgumentException(String.format("<remote> '%s' is neither a valid repositoryID nor a valid URL!", remote));
				}
			}
		}
	}

	@Override
	protected void assertLocalRootNotNull() {
		if (!isAll())
			super.assertLocalRootNotNull();
	}

	@Override
	public void run() throws Exception {
		if (isAll()) {
			for (EntityID repositoryID : LocalRepoRegistry.getInstance().getRepositoryIDs())
				sync(repositoryID);
		}
		else
			sync(localRoot);
	}

	private void sync(EntityID repositoryID) {
		File localRoot = LocalRepoRegistry.getInstance().getLocalRootOrFail(repositoryID);
		sync(localRoot);
	}

	private void sync(File localRoot) {
		List<URL> remoteRoots = new ArrayList<URL>();
		Map<EntityID, URL> filteredRemoteRepositoryID2RemoteRoot = new HashMap<EntityID, URL>();
		EntityID repositoryID;
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			if (localOnly) {
				localRepoManager.localSync(new LoggerProgressMonitor(logger));
				return;
			}

			repositoryID = localRepoManager.getRepositoryID();
			localRoot = localRepoManager.getLocalRoot();
			LocalRepoTransaction transaction = localRepoManager.beginTransaction();
			try {
				Collection<RemoteRepository> remoteRepositories = transaction.getDAO(RemoteRepositoryDAO.class).getObjects();
				for (RemoteRepository remoteRepository : remoteRepositories) {
					if (remoteRepository.getRemoteRoot() == null)
						continue;

					remoteRoots.add(remoteRepository.getRemoteRoot());
					if ((remoteRepositoryID == null && remoteRoot == null)
							|| (remoteRepositoryID != null && remoteRepositoryID.equals(remoteRepository.getEntityID()))
							|| (remoteRoot != null && remoteRoot.equals(remoteRepository.getRemoteRoot())))
						filteredRemoteRepositoryID2RemoteRoot.put(remoteRepository.getEntityID(), remoteRepository.getRemoteRoot());
				}

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			localRepoManager.close();
		}

		if (remoteRoots.isEmpty())
			System.err.println(String.format("WARNING: The repository %s ('%s') is not connected to any remote repository as client!", repositoryID, localRoot));
		else if (filteredRemoteRepositoryID2RemoteRoot.isEmpty())
			System.err.println(String.format("WARNING: The repository %s ('%s') is not connected to the specified remote repository ('%s')!", repositoryID, localRoot, remote));
		else {
			for (Map.Entry<EntityID, URL> me : filteredRemoteRepositoryID2RemoteRoot.entrySet()) {
				EntityID remoteRepositoryID = me.getKey();
				URL remoteRoot = me.getValue();
				System.out.println("********************************************************************************");
				System.out.println(String.format("Syncing %s ('%s') with %s ('%s').", repositoryID, localRoot, remoteRepositoryID, remoteRoot));
				System.out.println("********************************************************************************");
				RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRoot);
				repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			}
		}
	}

	private boolean isAll() {
		return "ALL".equals(local);
	}
}
