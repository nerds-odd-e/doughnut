package com.odde.donut.services.notebookGit;

import java.io.IOException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;

/**
 * Walks every object reachable from a head commit in one repository (commits, trees, blobs, and any
 * tags in between) and inserts each one into another repository's inserter, without going through a
 * parsed pack stream. {@link NotebookGitAcceptedRepositoryStore} uses this both to convert a
 * not-yet-touched binding's stored bundle bytes into the native object store on first {@code open},
 * and to bring a foreign source repository's history (for example an accepted proposal's imported
 * repository) into the native store on {@code store}. Public so {@code db.migration}'s Flyway
 * backfill migration - which runs outside Spring context - can reuse this exact copy mechanic
 * rather than a second implementation.
 */
public final class NotebookGitReachableObjectCopier {

  private NotebookGitReachableObjectCopier() {}

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
