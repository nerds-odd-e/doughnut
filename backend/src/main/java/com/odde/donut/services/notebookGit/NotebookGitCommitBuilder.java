package com.odde.donut.services.notebookGit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;

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
      ObjectId treeId = writeTree(tree, DirCache.newInCore(), inserter);
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
    try (ObjectInserter inserter = repository.newObjectInserter();
        RevWalk walk = new RevWalk(repository)) {
      DirCache parentTree =
          DirCache.read(walk.getObjectReader(), walk.parseCommit(parent).getTree());
      ObjectId treeId = writeTree(tree, parentTree, inserter);
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

  /** Writes each path as a regular file; inserts only blobs the parent tree does not hold. */
  private static ObjectId writeTree(
      NotebookGitTreeContent tree, DirCache parentTree, ObjectInserter inserter)
      throws IOException {
    Set<ObjectId> held = new HashSet<>();
    for (int i = 0; i < parentTree.getEntryCount(); i++) {
      held.add(parentTree.getEntry(i).getObjectId());
    }
    for (Map.Entry<ObjectId, byte[]> blob : tree.blobs().entrySet()) {
      if (!held.contains(blob.getKey())) {
        inserter.insert(Constants.OBJ_BLOB, blob.getValue());
      }
    }

    DirCacheBuilder builder = parentTree.builder();
    for (Map.Entry<String, ObjectId> entry : tree.blobIds().entrySet()) {
      DirCacheEntry dirCacheEntry = new DirCacheEntry(entry.getKey());
      dirCacheEntry.setFileMode(FileMode.REGULAR_FILE);
      dirCacheEntry.setObjectId(entry.getValue());
      builder.add(dirCacheEntry);
    }
    builder.finish();

    return parentTree.writeTree(inserter);
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
