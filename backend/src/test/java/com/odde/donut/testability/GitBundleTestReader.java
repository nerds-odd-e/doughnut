package com.odde.donut.testability;

import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
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

  /** The notebook content paths of a commit's tree, without reserved Git metadata. */
  public static List<String> pathsIn(Repository repository, ObjectId commitId) throws IOException {
    try (RevWalk revWalk = new RevWalk(repository)) {
      return readTreeEntries(repository, revWalk.parseCommit(commitId)).stream()
          .map(PortableTreeEntry::path)
          .toList();
    }
  }

  /**
   * The notebook content of a commit's tree as Portable tree entries (path plus exact bytes),
   * without reserved Git metadata such as {@code .gitattributes}.
   */
  public static List<PortableTreeEntry> readTreeEntries(Repository repository, RevCommit commit)
      throws IOException {
    return withoutMetadata(readTreeEntriesWithMetadata(repository, commit));
  }

  private static List<PortableTreeEntry> withoutMetadata(List<PortableTreeEntry> entries) {
    return entries.stream()
        .filter(entry -> !NotebookGitAttributes.isMetadataPath(entry.path()))
        .toList();
  }

  /**
   * Every blob reachable from a commit's tree as a Portable tree entry, including reserved Git
   * metadata, for tests whose subject is the exact tree or the metadata itself.
   */
  public static List<PortableTreeEntry> readTreeEntriesWithMetadata(
      Repository repository, RevCommit commit) throws IOException {
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
   * in-memory repository. Tests that also need the tip identity or first-parent ancestry use {@link
   * #fetchAcceptedTip(byte[])}.
   */
  public static List<PortableTreeEntry> fetchTipTreeEntries(byte[] bundleBytes)
      throws IOException, URISyntaxException {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      return readTreeEntriesWithMetadata(
          repository, revWalk.parseCommit(fetchHead(repository, bundleBytes)));
    }
  }

  /** The accepted tip, its first-parent ancestry, and its exact Portable tree content. */
  public static AcceptedTip fetchAcceptedTip(byte[] bundleBytes)
      throws IOException, URISyntaxException {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      RevCommit head = revWalk.parseCommit(fetchHead(repository, bundleBytes));
      List<ObjectId> ancestry = new ArrayList<>();
      for (RevCommit walked = head; walked.getParentCount() > 0; ) {
        walked = revWalk.parseCommit(walked.getParent(0));
        ancestry.add(walked.getId());
      }
      return new AcceptedTip(head.getId(), ancestry, readTreeEntriesWithMetadata(repository, head));
    }
  }

  public record AcceptedTip(
      ObjectId head, List<ObjectId> ancestry, List<PortableTreeEntry> entries) {}

  /**
   * The accepted history a bundle carries: every commit reachable from {@code refs/heads/main},
   * oldest last, plus the exact Portable content at the tip. Two snapshots are equal exactly when
   * the accepted history is unchanged. Serialized bundle bytes cannot answer that question: two
   * downloads of one unchanged history are re-serialized independently and need not be identical
   * byte for byte.
   */
  public static AcceptedHistory fetchAcceptedHistory(byte[] bundleBytes)
      throws IOException, URISyntaxException {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      RevCommit tip = revWalk.parseCommit(fetchHead(repository, bundleBytes));
      List<PortableTreeEntry> tipContent = readTreeEntriesWithMetadata(repository, tip);
      revWalk.markStart(tip);
      List<String> commits = new ArrayList<>();
      for (RevCommit commit : revWalk) {
        commits.add(commit.name());
      }
      return new AcceptedHistory(commits, tipContent, tip.getTree().getId());
    }
  }

  public record AcceptedHistory(
      List<String> commits, List<PortableTreeEntry> tipContent, ObjectId tipTreeId) {
    /** The parents this history advanced from: every commit but the newest. */
    public List<String> parents() {
      return commits.subList(1, commits.size());
    }

    /** The notebook content paths at the tip, without reserved Git metadata. */
    public List<String> tipPaths() {
      return withoutMetadata(tipContent).stream().map(PortableTreeEntry::path).toList();
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
