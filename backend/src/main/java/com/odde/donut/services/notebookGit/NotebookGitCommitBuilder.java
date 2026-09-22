package com.odde.donut.services.notebookGit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;

/**
 * Commits a {@link NotebookGitTreeContent} on {@code refs/heads/main}: {@link #build} makes a fresh
 * in-memory repository with a single, parentless root commit; {@link #append} adds a commit on a
 * given parent.
 *
 * <p>No filesystem writes and no Donut identity: the caller supplies author/message/time, and the
 * notebook ID (if any) stays the caller's concern rather than being embedded in paths or blobs.
 */
public final class NotebookGitCommitBuilder {

  private NotebookGitCommitBuilder() {}

  public static Repository build(
      NotebookGitTreeContent tree,
      String authorName,
      String authorEmail,
      String message,
      Instant commitTime) {
    InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
    try (ObjectInserter inserter = repository.newObjectInserter()) {
      ObjectId treeId = writeTree(tree, inserter);
      ObjectId commitId =
          inserter.insert(commitBuilder(treeId, authorName, authorEmail, message, commitTime));
      inserter.flush();
      updateMainBranch(repository, commitId);
      return repository;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static ObjectId append(
      Repository repository,
      ObjectId parent,
      NotebookGitTreeContent tree,
      String authorName,
      String authorEmail,
      String message,
      Instant commitTime) {
    try (ObjectInserter inserter = repository.newObjectInserter()) {
      ObjectId treeId = writeTree(tree, inserter);
      CommitBuilder commitBuilder =
          commitBuilder(treeId, authorName, authorEmail, message, commitTime);
      commitBuilder.setParentId(parent);
      ObjectId commitId = inserter.insert(commitBuilder);
      inserter.flush();
      updateMainBranch(repository, commitId);
      return commitId;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Inserts each supplied blob and tree object and returns the content's native root tree id.
   * Existing batched storage deduplicates objects the store already holds.
   */
  private static ObjectId writeTree(NotebookGitTreeContent tree, ObjectInserter inserter)
      throws IOException {
    for (Map.Entry<ObjectId, byte[]> blob : tree.blobs().entrySet()) {
      inserter.insert(Constants.OBJ_BLOB, blob.getValue());
    }
    for (Map.Entry<ObjectId, byte[]> treeObject : tree.trees().entrySet()) {
      inserter.insert(Constants.OBJ_TREE, treeObject.getValue());
    }
    return tree.treeId();
  }

  private static CommitBuilder commitBuilder(
      ObjectId treeId, String authorName, String authorEmail, String message, Instant commitTime) {
    PersonIdent ident = new PersonIdent(authorName, authorEmail, commitTime, ZoneOffset.UTC);
    CommitBuilder commitBuilder = new CommitBuilder();
    commitBuilder.setTreeId(treeId);
    commitBuilder.setAuthor(ident);
    commitBuilder.setCommitter(ident);
    commitBuilder.setMessage(message);
    return commitBuilder;
  }

  private static void updateMainBranch(Repository repository, ObjectId commitId)
      throws IOException {
    RefUpdate refUpdate = repository.updateRef(Constants.R_HEADS + "main");
    refUpdate.setNewObjectId(commitId);
    RefUpdate.Result result = refUpdate.update();
    if (result != RefUpdate.Result.NEW && result != RefUpdate.Result.FAST_FORWARD) {
      throw new IllegalStateException("Unexpected ref update result: " + result);
    }
  }
}
