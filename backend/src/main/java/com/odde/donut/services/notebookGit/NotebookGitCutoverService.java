package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookExport.NotebookLivePortableTree;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives one notebook its accepted Git binding: builds the notebook's canonical Portable-tree
 * snapshot as of a given commit time, commits it as a single parentless root commit under a stable
 * Donut system identity, and persists the accepted binding.
 *
 * <p>{@code NotebookService} calls {@link #createBindingForNotebook} at creation time so every
 * notebook starts Git-backed from an empty tree; {@link #resetHistory} writes the same kind of root
 * commit over a binding a notebook already has. The caller supplies the commit time, and a tree
 * failure propagates before a binding is persisted.
 */
@Service
public class NotebookGitCutoverService {

  /** Stable system identity used for Donut-generated Git commits such as this cutover. */
  public static final String SYSTEM_AUTHOR_NAME = "Donut System";

  public static final String SYSTEM_AUTHOR_EMAIL = "system@donut.local";

  static final String CUTOVER_COMMIT_MESSAGE =
      "Cutover: snapshot existing notebook content into Git";

  static final String RESET_COMMIT_MESSAGE = "Reset: restart Git history from the current notebook";

  private final NotebookLivePortableTree livePortableTree;
  private final NotebookGitBindingRepository notebookGitBindingRepository;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public NotebookGitCutoverService(
      NotebookLivePortableTree livePortableTree,
      NotebookGitBindingRepository notebookGitBindingRepository,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.livePortableTree = livePortableTree;
    this.notebookGitBindingRepository = notebookGitBindingRepository;
    this.repositoryStore = repositoryStore;
  }

  public NotebookGitBinding createBindingForNotebook(Notebook notebook, Instant cutoverTime) {
    NotebookGitBinding binding = new NotebookGitBinding();
    binding.setNotebook(notebook);
    try (Repository repository = buildRepository(notebook, cutoverTime, CUTOVER_COMMIT_MESSAGE)) {
      storeHistory(binding, repository, cutoverTime);
    }
    return notebookGitBindingRepository.save(binding);
  }

  /**
   * Restarts {@code notebook}'s accepted Git history: one parentless commit of the notebook's
   * current content replaces whatever the binding held, so the notebook can be cloned and published
   * again whatever state its history was in.
   */
  @Transactional
  public NotebookGitBinding resetHistory(Notebook notebook, Instant resetTime) {
    NotebookGitBinding binding =
        notebookGitBindingRepository
            .findByNotebookIdForUpdate(notebook.getId())
            .orElseGet(
                () -> {
                  NotebookGitBinding created = new NotebookGitBinding();
                  created.setNotebook(notebook);
                  return created;
                });
    try (Repository repository = buildRepository(notebook, resetTime, RESET_COMMIT_MESSAGE)) {
      storeHistory(binding, repository, resetTime);
    }
    return notebookGitBindingRepository.save(binding);
  }

  private Repository buildRepository(Notebook notebook, Instant commitTime, String message) {
    List<PortableTreeEntry> entries = livePortableTree.entriesOf(notebook);
    return NotebookGitBundleBuilder.build(
        entries, SYSTEM_AUTHOR_NAME, SYSTEM_AUTHOR_EMAIL, message, commitTime);
  }

  private void storeHistory(NotebookGitBinding binding, Repository repository, Instant time) {
    Timestamp timestamp = Timestamp.from(time);
    if (binding.getCreatedAt() == null) {
      binding.setCreatedAt(timestamp);
    }
    repositoryStore.apply(binding, repository, timestamp);
  }
}
