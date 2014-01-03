package co.codewizards.cloudstore.webapp.service;

import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSetRequest;
import co.codewizards.cloudstore.core.dto.ChangeSetResponse;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

@Path("ChangeSet")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class ChangeSetService
{
	private static final Logger logger = LoggerFactory.getLogger(ChangeSetService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@POST
	@Path("{repositoryID}")
	public ChangeSetResponse getChangeSet(@PathParam("repositoryID") EntityID repositoryID, ChangeSetRequest request)
	{
		URL repoURL = getRepositoryURL(repositoryID);
		RepoTransportFactoryRegistry repoTransportRegistry = RepoTransportFactoryRegistry.getInstance();
		RepoTransportFactory repoTransportFactory = repoTransportRegistry.getRepoTransportFactory(repoURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(repoURL);
		ChangeSetResponse response = repoTransport.getChangeSet(request);
		return response;
	}

	private URL getRepositoryURL(EntityID repositoryID) {
		LocalRepoRegistry registry = LocalRepoRegistry.getInstance();
		throw new UnsupportedOperationException("NYI");
	}
}
