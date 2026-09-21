package com.odde.donut.services.notebookGit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

/**
 * Imports a Git bundle's {@code main} history into an in-memory repository. Public so {@code
 * db.migration}'s one-time backfill migration - which runs outside Spring context and cannot depend
 * on any Spring-managed bean - can import a pre-upgrade binding's bundle bytes with the same
 * importer that reads proposal bundles.
 */
public final class NotebookGitBundleImporter {
  private NotebookGitBundleImporter() {}

  public record ImportedBundle(Repository repository, ObjectId mainHead) implements AutoCloseable {
    @Override
    public void close() {
      repository.close();
    }
  }

  public static ImportedBundle importMainHead(byte[] bundleBytes, String sourceName) {
    InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
    try {
      ObjectId mainHead = fetchMainHead(repository, bundleBytes, sourceName);
      return new ImportedBundle(repository, mainHead);
    } catch (IOException e) {
      repository.close();
      throw new UncheckedIOException("Could not import " + sourceName, e);
    } catch (URISyntaxException | RuntimeException e) {
      repository.close();
      throw new IllegalStateException("Could not import " + sourceName, e);
    }
  }

  /**
   * {@link #importMainHead} plus the check the one-time backfill migration needs when it imports a
   * pre-upgrade binding's stored bundle: the bundle's {@code main} must equal {@code expectedHead},
   * since the bundle bytes are only an encoding of that already-accepted history, never an
   * independent source of truth for it.
   */
  public static ImportedBundle importAndVerifyMainHead(
      byte[] bundleBytes, String sourceName, ObjectId expectedHead) {
    ImportedBundle imported = importMainHead(bundleBytes, sourceName);
    if (!imported.mainHead().equals(expectedHead)) {
      imported.close();
      throw new IllegalStateException(
          sourceName + "'s bundle main does not match its persisted head");
    }
    return imported;
  }

  private static ObjectId fetchMainHead(
      InMemoryRepository repository, byte[] bundleBytes, String sourceName)
      throws IOException, URISyntaxException {
    try (TransportBundleStream transport =
            new TransportBundleStream(
                repository,
                new URIish("in-memory:" + sourceName),
                new ByteArrayInputStream(bundleBytes));
        FetchConnection fetchConnection = transport.openFetch()) {
      Ref mainRef = fetchConnection.getRef("refs/heads/main");
      if (mainRef == null) {
        throw new IllegalStateException(sourceName + " does not advertise refs/heads/main");
      }
      fetchConnection.fetch(NullProgressMonitor.INSTANCE, List.of(mainRef), Set.of());
      RefUpdate localMain = repository.updateRef("refs/heads/main");
      localMain.setNewObjectId(mainRef.getObjectId());
      localMain.forceUpdate();
      return mainRef.getObjectId();
    }
  }
}
