package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
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

/**
 * Turns a canonical Portable-tree snapshot (see {@code notebookExport.PortableTreeSnapshot} into an
 * in-memory Git repository with a single, parentless root commit on {@code refs/heads/main}.
 *
 * <p>No filesystem writes and no Donut identity: the caller supplies author/message/time, and the
 * notebook ID (if any) stays the caller's concern rather than being embedded in paths or blobs.
 */
public final class NotebookGitBundleBuilder {

  private NotebookGitBundleBuilder() {}

  public static Repository build(
      List<PortableTreeEntry> entries,
      String authorName,
      String authorEmail,
      String message,
      Instant commitTime) {
    InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
    try (ObjectInserter inserter = repository.newObjectInserter()) {
      ObjectId treeId = writeTree(entries, inserter);
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
    try (ObjectInserter inserter = repository.newObjectInserter()) {
      ObjectId treeId = writeTree(entries, inserter);
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

  private static ObjectId writeTree(List<PortableTreeEntry> entries, ObjectInserter inserter)
      throws IOException {
    DirCache dirCache = DirCache.newInCore();
    DirCacheBuilder builder = dirCache.builder();

    List<PortableTreeEntry> sortedByPath =
        entries.stream().sorted(Comparator.comparing(PortableTreeEntry::path)).toList();
    for (PortableTreeEntry entry : sortedByPath) {
      ObjectId blobId =
          inserter.insert(Constants.OBJ_BLOB, entry.content().getBytes(StandardCharsets.UTF_8));
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
    PersonIdent author = new PersonIdent(authorName, authorEmail, commitTime, ZoneOffset.UTC);
    CommitBuilder commitBuilder = new CommitBuilder();
    commitBuilder.setTreeId(treeId);
    commitBuilder.setAuthor(author);
    commitBuilder.setCommitter(author);
    commitBuilder.setMessage(message);
    return commitBuilder;
  }

  private static void updateMainBranch(Repository repository, ObjectId commitId)
      throws IOException {
    RefUpdate refUpdate = repository.updateRef(Constants.R_HEADS + "main");
    refUpdate.setNewObjectId(commitId);
    RefUpdate.Result result = refUpdate.update();
    if (result != RefUpdate.Result.NEW
        && result != RefUpdate.Result.FORCED
        && result != RefUpdate.Result.FAST_FORWARD) {
      throw new IllegalStateException("Unexpected ref update result: " + result);
    }
  }
}
