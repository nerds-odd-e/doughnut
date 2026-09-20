package com.odde.donut.services.notebookGit.objectstore;

import java.io.IOException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;

/**
 * Modeled on JGit's own {@code DfsRefUpdate}: the JGit base class already handles fast-forward
 * detection and result classification (via its own internal {@code RevWalk}), so this class only
 * needs to attempt one compare-and-put against the binding row and report whether it won the race.
 * No locking is taken here - the SQL {@code UPDATE ... WHERE accepted_git_object_id = ?} itself is
 * the compare-and-swap, so there is no in-process cache entry to update afterward.
 */
final class JdbcNotebookRefUpdate extends RefUpdate {

  private final JdbcNotebookRefDatabase refDatabase;
  private final Repository repository;
  private Ref dstRef;

  JdbcNotebookRefUpdate(JdbcNotebookRefDatabase refDatabase, Ref ref, Repository repository) {
    super(ref);
    this.refDatabase = refDatabase;
    this.repository = repository;
  }

  @Override
  protected JdbcNotebookRefDatabase getRefDatabase() {
    return refDatabase;
  }

  @Override
  protected Repository getRepository() {
    return repository;
  }

  @Override
  protected boolean tryLock(boolean deref) {
    dstRef = getRef();
    if (deref) {
      dstRef = dstRef.getLeaf();
    }
    if (dstRef.isSymbolic()) {
      setOldObjectId(null);
    } else {
      setOldObjectId(dstRef.getObjectId());
    }
    return true;
  }

  @Override
  protected void unlock() {
    // No state is held while "locked"; the SQL UPDATE's WHERE clause is the compare-and-swap.
  }

  @Override
  protected Result doUpdate(Result desiredResult) throws IOException {
    boolean applied =
        refDatabase.compareAndPutMain(dstRef.getName(), dstRef.getObjectId(), getNewObjectId());
    return applied ? desiredResult : Result.LOCK_FAILURE;
  }

  @Override
  protected Result doDelete(Result desiredResult) {
    // The binding's accepted_git_object_id column is NOT NULL: this store's one branch can never
    // be deleted through a ref update, only by removing the whole binding row.
    return Result.LOCK_FAILURE;
  }

  @Override
  protected Result doLink(String target) {
    throw new UnsupportedOperationException(
        "HEAD always targets refs/heads/main in this single-branch store; relinking it to "
            + target
            + " is not supported.");
  }
}
