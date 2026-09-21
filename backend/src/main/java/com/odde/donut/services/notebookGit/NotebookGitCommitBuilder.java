package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
 * Commits a canonical Portable-tree snapshot (see {@code notebookExport.PortableTreeSnapshot}) on
 * {@code refs/heads/main}: {@link #build} makes a fresh in-memory repository with a single,
 * parentless root commit; {@link #append} adds a commit on a given parent.
 *
 * <p>No filesystem writes and no Donut identity: the caller supplies author/message/time, and the
 * notebook ID (if any) stays the caller's concern rather than being embedded in paths or blobs.
 */
public final class NotebookGitCommitBuilder {

  private NotebookGitCommitBuilder() {}

  public static Repository build(
      List<PortableTreeEntry> entries,
      String authorName,
      String authorEmail,
      String message,
      Instant commitTime) {
    InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
    try (ObjectInserter inserter = repository.newObjectInserter()) {
      ObjectId treeId = writeTree(entries, DirCache.newInCore(), inserter);
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
      List<PortableTreeEntry> entries,
      String authorName,
      String authorEmail,
      String message,
      Instant commitTime) {
    try (ObjectInserter inserter = repository.newObjectInserter();
        RevWalk walk = new RevWalk(repository)) {
      DirCache dirCache = DirCache.read(walk.getObjectReader(), walk.parseCommit(parent).getTree());
      ObjectId treeId = writeTree(entries, dirCache, inserter);
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

  /** Writes each entry as a regular file; inserts only blobs the parent lacks at that path. */
  private static ObjectId writeTree(
      List<PortableTreeEntry> entries, DirCache dirCache, ObjectInserter inserter)
      throws IOException {
    DirCacheBuilder builder = dirCache.builder();

    for (PortableTreeEntry entry : entries) {
      ObjectId blobId = inserter.idFor(Constants.OBJ_BLOB, entry.content());
      DirCacheEntry accepted = dirCache.getEntry(entry.path());
      if (accepted == null || !blobId.equals(accepted.getObjectId())) {
        inserter.insert(Constants.OBJ_BLOB, entry.content());
      }
      DirCacheEntry dirCacheEntry = new DirCacheEntry(entry.path());
      dirCacheEntry.setFileMode(FileMode.REGULAR_FILE);
      dirCacheEntry.setObjectId(blobId);
      builder.add(dirCacheEntry);
    }
    builder.finish();

    return dirCache.writeTree(inserter);
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
