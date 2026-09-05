package com.odde.donut.services.notebookGit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.TransportBundleStream;
import org.eclipse.jgit.transport.URIish;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Imports a client-submitted proposal Git bundle into a fresh in-memory repository, reusing the
 * same {@code TransportBundleStream} fetch used to read back an accepted binding's bundle (see
 * {@code GitBundleTestReader}). Unlike that test-only reader, a proposal comes from an untrusted
 * client, so any failure to produce a complete, usable {@code refs/heads/main} - corrupt bytes, a
 * missing ref, or a bundle that doesn't actually carry every object it advertises reachability to -
 * is converted into an actionable {@link ResponseStatusException} rather than left to propagate as
 * a raw JGit exception.
 */
public final class NotebookGitProposalImporter {

  private NotebookGitProposalImporter() {}

  /** The fresh in-memory repository the bundle was imported into, and its resolved main head. */
  public record ImportedProposal(Repository repository, ObjectId mainHead) {}

  public static ImportedProposal importMainHead(byte[] bundleBytes) {
    InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
    try {
      return new ImportedProposal(repository, fetchMainHead(repository, bundleBytes));
    } catch (ResponseStatusException e) {
      repository.close();
      throw e;
    } catch (IOException | URISyntaxException | RuntimeException e) {
      repository.close();
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Proposal bundle is unreadable or does not carry a complete refs/heads/main.",
          e);
    }
  }

  private static ObjectId fetchMainHead(InMemoryRepository repository, byte[] bundleBytes)
      throws IOException, URISyntaxException {
    try (TransportBundleStream transport =
            new TransportBundleStream(
                repository,
                new URIish("in-memory:proposal-bundle"),
                new ByteArrayInputStream(bundleBytes));
        FetchConnection fetchConnection = transport.openFetch()) {
      Ref mainRef = fetchConnection.getRef("refs/heads/main");
      if (mainRef == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Proposal bundle does not advertise refs/heads/main.");
      }
      fetchConnection.fetch(NullProgressMonitor.INSTANCE, List.of(mainRef), Set.of());
      RefUpdate localMain = repository.updateRef("refs/heads/main");
      localMain.setNewObjectId(mainRef.getObjectId());
      localMain.forceUpdate();
      return mainRef.getObjectId();
    }
  }
}
