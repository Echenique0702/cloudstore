package co.codewizards.cloudstore.core.repo.local;

public interface LocalRepoTransaction {

	void commit();

	boolean isActive();

	void rollback();

	void rollbackIfActive();

	long getLocalRevision();

	LocalRepoManager getLocalRepoManager();

	<D> D getDao(Class<D> daoClass);

	void flush();

}
