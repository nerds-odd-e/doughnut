package com.odde.donut.testability;

import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.TransportBundleStream;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;

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

  public static SingleParentGitCommit fetchSingleParentCommit(
      InMemoryRepository target, byte[] bundleBytes) throws IOException, URISyntaxException {
    ObjectId head = fetchHead(target, bundleBytes);
    try (RevWalk revWalk = new RevWalk(target)) {
      RevCommit commit = revWalk.parseCommit(head);
      return new SingleParentGitCommit(head, commit.getTree().getId(), commit.getParent(0).getId());
    }
  }

  public record SingleParentGitCommit(ObjectId head, ObjectId tree, ObjectId parent) {}

  public static ObjectId blobIdAt(Repository repository, ObjectId commitId, String path)
      throws IOException {
    try (RevWalk revWalk = new RevWalk(repository);
        TreeWalk treeWalk =
            TreeWalk.forPath(repository, path, revWalk.parseCommit(commitId).getTree())) {
      if (treeWalk == null) {
        throw new IOException("missing path: " + path);
      }
      return treeWalk.getObjectId(0);
    }
  }

  public static List<String> pathsIn(Repository repository, ObjectId commitId) throws IOException {
    try (RevWalk revWalk = new RevWalk(repository);
        TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.addTree(revWalk.parseCommit(commitId).getTree());
      treeWalk.setRecursive(true);
      List<String> paths = new ArrayList<>();
      while (treeWalk.next()) {
        paths.add(treeWalk.getPathString());
      }
      return paths;
    }
  }

  /**
   * Reads every blob reachable from a commit's tree as a Portable tree entry (path plus exact
   * bytes), for tests comparing a read-back Git tree against expected Portable-tree content.
   */
  public static List<PortableTreeEntry> readTreeEntries(Repository repository, RevCommit commit)
      throws IOException {
    List<PortableTreeEntry> entries = new ArrayList<>();
    try (TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.addTree(commit.getTree());
      treeWalk.setRecursive(true);
      while (treeWalk.next()) {
        ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
        entries.add(new PortableTreeEntry(treeWalk.getPathString(), loader.getBytes()));
      }
    }
    return entries;
  }

  /**
   * The Portable tree entries at the tip of a persisted binding's bundle, read through a scratch
   * in-memory repository. For tests that only need the accepted tip's content; a test that also
   * needs the tip commit itself opens its own repository and uses {@link #readTreeEntries}.
   */
  public static List<PortableTreeEntry> fetchTipTreeEntries(byte[] bundleBytes)
      throws IOException, URISyntaxException {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      return readTreeEntries(repository, revWalk.parseCommit(fetchHead(repository, bundleBytes)));
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

  /**
   * Walks every object reachable from {@code head} in {@code source} (commits, trees, blobs, and
   * any tags in between) and inserts each one into {@code target}, so a test can seed a fresh store
   * from a fixture repository without going through a parsed pack stream.
   */
  public static void copyAllReachableObjects(
      Repository source, AnyObjectId head, ObjectInserter target) throws IOException {
    try (ObjectWalk walk = new ObjectWalk(source)) {
      RevCommit start = walk.parseCommit(head);
      walk.markStart(start);
      RevCommit commit;
      while ((commit = walk.next()) != null) {
        copyOne(source, target, commit);
      }
      RevObject object;
      while ((object = walk.nextObject()) != null) {
        copyOne(source, target, object);
      }
    }
  }

  private static void copyOne(Repository source, ObjectInserter target, AnyObjectId id)
      throws IOException {
    ObjectLoader loader = source.open(id);
    target.insert(loader.getType(), loader.getBytes());
  }
}
