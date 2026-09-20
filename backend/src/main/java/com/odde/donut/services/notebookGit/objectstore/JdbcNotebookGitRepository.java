package com.odde.donut.services.notebookGit.objectstore;

import java.sql.Connection;
import java.util.Collections;
import org.eclipse.jgit.attributes.AttributesNode;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.attributes.AttributesRule;
import org.eclipse.jgit.lib.BaseRepositoryBuilder;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

/**
 * A JGit {@link Repository} whose objects and single {@code refs/heads/main} ref live in the {@code
 * notebook_git_accepted_object} table and the owning {@link
 * com.odde.donut.entities.NotebookGitBinding}'s {@code accepted_git_object_id} column, keyed by one
 * {@code notebookGitBindingId}. Not a {@code DfsRepository}: this design deliberately bypasses
 * JGit's pack format entirely, so every write goes through {@link
 * org.eclipse.jgit.lib.ObjectInserter#insert} calls, never a parsed pack stream.
 *
 * <p>This repository has no local filesystem footprint (bare, no {@code gitDir}/work tree) and no
 * process-local cache: every read goes through the supplied JDBC {@link Connection}, so closing and
 * reopening this repository (even a fresh instance) always reflects the connection's current
 * transactional view, never a stale in-memory snapshot.
 *
 * <p>The caller owns the {@link Connection}'s lifecycle and transaction boundaries (autocommit,
 * commit, rollback, close); this repository never closes it.
 */
public final class JdbcNotebookGitRepository extends Repository {

  private static final AttributesNodeProvider EMPTY_ATTRIBUTES_NODE_PROVIDER =
      new EmptyAttributesNodeProvider();

  private final int notebookGitBindingId;
  private final JdbcNotebookObjectDatabase objectDatabase;
  private final JdbcNotebookRefDatabase refDatabase;
  private final StoredConfig config = new InMemoryStoredConfig();

  public JdbcNotebookGitRepository(int notebookGitBindingId, Connection connection) {
    super(new BaseRepositoryBuilder());
    this.notebookGitBindingId = notebookGitBindingId;
    this.objectDatabase = new JdbcNotebookObjectDatabase(notebookGitBindingId, connection);
    this.refDatabase = new JdbcNotebookRefDatabase(this, notebookGitBindingId, connection);
  }

  @Override
  public void create(boolean bare) {
    throw new UnsupportedOperationException(
        "A NotebookGitBinding row establishes the initial accepted head; this adapter does not"
            + " initialize new repositories.");
  }

  @Override
  public String getIdentifier() {
    return "notebook-git-binding:" + notebookGitBindingId;
  }

  @Override
  public JdbcNotebookObjectDatabase getObjectDatabase() {
    return objectDatabase;
  }

  @Override
  public JdbcNotebookRefDatabase getRefDatabase() {
    return refDatabase;
  }

  @Override
  public StoredConfig getConfig() {
    return config;
  }

  @Override
  public AttributesNodeProvider createAttributesNodeProvider() {
    return EMPTY_ATTRIBUTES_NODE_PROVIDER;
  }

  @Override
  public void scanForRepoChanges() {
    // No local filesystem state to rescan.
  }

  @Override
  public void notifyIndexChanged(boolean internal) {
    // Bare repository: no working-tree index exists.
  }

  private static final class InMemoryStoredConfig extends StoredConfig {
    @Override
    public void load() {
      // Nothing persisted: this repository carries no repository-level config.
    }

    @Override
    public void save() {
      // Nothing to persist.
    }
  }

  private static final class EmptyAttributesNodeProvider implements AttributesNodeProvider {
    private final AttributesNode empty = new EmptyAttributesNode();

    @Override
    public AttributesNode getInfoAttributesNode() {
      return empty;
    }

    @Override
    public AttributesNode getGlobalAttributesNode() {
      return empty;
    }

    private static final class EmptyAttributesNode extends AttributesNode {
      EmptyAttributesNode() {
        super(Collections.<AttributesRule>emptyList());
      }
    }
  }
}
