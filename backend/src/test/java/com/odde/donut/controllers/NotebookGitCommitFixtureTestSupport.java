package com.odde.donut.controllers;

import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.BundleWriter;

/**
 * Raw JGit commit-crafting helpers shared by notebook Git controller tests: building a commit
 * directly against a {@link Repository} (bypassing {@code NotebookGitCommitBuilder}) and
 * serializing a chosen head into bundle bytes, for tests that need to construct a specific
 * tree/history shape rather than one produced by the product's own save path. These do not depend
 * on the notebook/JPA fixtures {@link NotebookGitControllerTestBase} adds on top.
 */
abstract class NotebookGitCommitFixtureTestSupport extends NoteDependentRowsControllerTestBase {

  /**
   * A bundle whose {@code main} is a single-parent child of the tip in {@code currentBundle}, with
   * {@code files} as its tree plus the tip's reserved Git metadata that {@code files} do not name,
   * as a local checkout would keep it.
   */
  static byte[] proposalBundleBytes(byte[] currentBundle, List<NotebookGitProposalFile> files)
      throws IOException, URISyntaxException {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, currentBundle);
      ObjectId childCommit =
          commitOnTopOf(
              repository,
              List.of(acceptedHead),
              withAcceptedMetadata(repository, acceptedHead, files),
              "Proposal");
      return bundleBytesForHead(repository, childCommit);
    }
  }

  /** {@code files} plus the reserved Git metadata of {@code acceptedHead} they do not name. */
  static List<NotebookGitProposalFile> withAcceptedMetadata(
      Repository repository, ObjectId acceptedHead, List<NotebookGitProposalFile> files)
      throws IOException {
    Set<String> named =
        files.stream().map(NotebookGitProposalFile::path).collect(Collectors.toSet());
    try (RevWalk revWalk = new RevWalk(repository)) {
      return Stream.concat(
              NotebookGitAttributes.selectFrom(
                      GitBundleTestReader.readTreeEntries(
                          repository, revWalk.parseCommit(acceptedHead)))
                  .stream()
                  .filter(entry -> !named.contains(entry.path()))
                  .map(entry -> new NotebookGitProposalFile(entry.path(), entry.content())),
              files.stream())
          .toList();
    }
  }

  static ObjectId commitOnTopOf(
      Repository repository, List<ObjectId> parents, String path, String content, String message)
      throws IOException {
    return commitOnTopOf(
        repository, parents, List.of(new NotebookGitProposalFile(path, content)), message);
  }

  static ObjectId commitOnTopOf(
      Repository repository,
      List<ObjectId> parents,
      List<NotebookGitProposalFile> files,
      String message)
      throws IOException {
    try (ObjectInserter inserter = repository.newObjectInserter()) {
      DirCache dirCache = DirCache.newInCore();
      DirCacheBuilder builder = dirCache.builder();
      List<NotebookGitProposalFile> sortedByPath =
          files.stream().sorted(Comparator.comparing(NotebookGitProposalFile::path)).toList();
      for (NotebookGitProposalFile file : sortedByPath) {
        ObjectId blobId = inserter.insert(Constants.OBJ_BLOB, file.contentBytes());
        DirCacheEntry entry = new DirCacheEntry(file.path());
        entry.setFileMode(file.mode());
        entry.setObjectId(blobId);
        builder.add(entry);
      }
      builder.finish();
      ObjectId treeId = dirCache.writeTree(inserter);

      PersonIdent author =
          new PersonIdent("Proposer", "proposer@example.com", Instant.now(), ZoneOffset.UTC);
      CommitBuilder commitBuilder = new CommitBuilder();
      commitBuilder.setTreeId(treeId);
      commitBuilder.setParentIds(parents.toArray(new ObjectId[0]));
      commitBuilder.setAuthor(author);
      commitBuilder.setCommitter(author);
      commitBuilder.setMessage(message);
      ObjectId commitId = inserter.insert(commitBuilder);
      inserter.flush();
      return commitId;
    }
  }

  static byte[] bundleBytesForHead(Repository repository, ObjectId headId) throws IOException {
    RefUpdate refUpdate = repository.updateRef(Constants.R_HEADS + "main");
    refUpdate.setNewObjectId(headId);
    refUpdate.forceUpdate();

    BundleWriter bundleWriter = new BundleWriter(repository);
    bundleWriter.include(Constants.R_HEADS + "main", headId);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bundleWriter.writeBundle(NullProgressMonitor.INSTANCE, out);
    return out.toByteArray();
  }

  /**
   * A frontmatter-valid baseline tree: one typed note, one typed README - clears tree-shape and
   * reaches the typed-Markdown gate, for tests whose proposal must get past both.
   */
  static List<PortableTreeEntry> validBaselineEntries() {
    return List.of(
        PortableTreeEntry.ofText("note.md", "---\ntype: Note\n---\noriginal content"),
        PortableTreeEntry.ofText("README.md", "---\ntype: Readme\n---\nreadme original"));
  }
}
