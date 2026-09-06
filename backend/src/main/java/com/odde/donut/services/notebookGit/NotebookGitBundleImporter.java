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

final class NotebookGitBundleImporter {
  private NotebookGitBundleImporter() {}

  record ImportedBundle(Repository repository, ObjectId mainHead) implements AutoCloseable {
    @Override
    public void close() {
      repository.close();
    }
  }

  static ImportedBundle importMainHead(byte[] bundleBytes, String sourceName) {
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
