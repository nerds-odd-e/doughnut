package com.odde.donut.testability;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.TransportBundleStream;
import org.eclipse.jgit.transport.URIish;

/**
 * Fetches an accepted Git binding's bundle bytes into a scratch in-memory repository so a test can
 * inspect {@code refs/heads/main} with JGit. Shared by every test that reads back a persisted
 * {@code NotebookGitBinding}.
 */
public final class GitBundleTestReader {

  private GitBundleTestReader() {}

  public static ObjectId fetchHead(InMemoryRepository target, byte[] bundleBytes)
      throws IOException, URISyntaxException {
    try (TransportBundleStream transport =
            new TransportBundleStream(
                target, new URIish("in-memory:bundle"), new ByteArrayInputStream(bundleBytes));
        FetchConnection fetchConnection = transport.openFetch()) {
      Ref mainRef = fetchConnection.getRef("refs/heads/main");
      fetchConnection.fetch(NullProgressMonitor.INSTANCE, List.of(mainRef), Set.of());
      return mainRef.getObjectId();
    }
  }

  /**
   * The bundle's advertised {@code HEAD} object id, or {@code null} if the bundle never included
   * one. A system {@code git clone} of a bundle without this uses the cloning machine's own {@code
   * init.defaultBranch} to name the checked-out branch instead of {@code main}, so every bundle
   * this service hands to the CLI must advertise it.
   */
  public static ObjectId fetchAdvertisedHead(byte[] bundleBytes)
      throws IOException, URISyntaxException {
    try (InMemoryRepository scratch = new InMemoryRepository(new DfsRepositoryDescription());
        TransportBundleStream transport =
            new TransportBundleStream(
                scratch,
                new URIish("in-memory:bundle-head-check"),
                new ByteArrayInputStream(bundleBytes));
        FetchConnection fetchConnection = transport.openFetch()) {
      Ref headRef = fetchConnection.getRef(Constants.HEAD);
      return headRef == null ? null : headRef.getObjectId();
    }
  }
}
