package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives one notebook its accepted Git binding: builds the notebook's canonical Portable-tree
 * snapshot as of a given commit time, commits it as a single parentless root commit under a stable
 * Donut system identity, and persists the accepted binding.
 *
 * <p>{@code NotebookService} calls {@link #createBindingForNotebook} at creation time so every
 * notebook starts Git-backed from an empty content tree with LFS representation and initial {@code
 * .gitattributes}; {@link #resetHistory} writes the same kind of root commit over a binding a
 * notebook already has, preserving that binding's representation and accepted metadata (or, for a
 * notebook without one, creating it as at creation time). The caller supplies the commit time, and
 * a tree failure propagates before a binding is persisted.
 */
@Service
public class NotebookGitCutoverService {

  /** Stable system identity used for Donut-generated Git commits such as this cutover. */
  public static final String SYSTEM_AUTHOR_NAME = "Donut System";

  public static final String SYSTEM_AUTHOR_EMAIL = "system@donut.local";

  static final String CUTOVER_COMMIT_MESSAGE =
      "Cutover: snapshot existing notebook content into Git";

  static final String RESET_COMMIT_MESSAGE = "Reset: restart Git history from the current notebook";

  private final NotebookGitTreeEncoder treeEncoder;
  private final NotebookGitBindingRepository notebookGitBindingRepository;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public NotebookGitCutoverService(
      NotebookGitTreeEncoder treeEncoder,
      NotebookGitBindingRepository notebookGitBindingRepository,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.treeEncoder = treeEncoder;
    this.notebookGitBindingRepository = notebookGitBindingRepository;
    this.repositoryStore = repositoryStore;
  }

  public NotebookGitBinding createBindingForNotebook(Notebook notebook, Instant cutoverTime) {
    NotebookGitBinding binding = newBinding(notebook);
    try (Repository repository =
        buildRepository(
            notebook,
            cutoverTime,
            CUTOVER_COMMIT_MESSAGE,
            NotebookGitAttributes.initialMetadata())) {
      storeHistory(binding, repository, cutoverTime);
    }
    return binding;
  }

  /**
   * Restarts {@code notebook}'s accepted Git history: one parentless commit of the notebook's
   * current content replaces whatever the binding held, so the notebook can be cloned and published
   * again whatever state its history was in. Accepted Git metadata such as {@code .gitattributes}
   * is preserved exactly rather than regenerated from defaults; a notebook without a binding gets a
   * new LFS binding with initial {@code .gitattributes}, as at creation.
   */
  @Transactional
  public NotebookGitBinding resetHistory(Notebook notebook, Instant resetTime) {
    return notebookGitBindingRepository
        .findByNotebookIdForUpdate(notebook.getId())
        .map(binding -> resetHistory(notebook, binding, resetTime, acceptedMetadata(binding)))
        .orElseGet(
            () ->
                resetHistory(
                    notebook,
                    newBinding(notebook),
                    resetTime,
                    NotebookGitAttributes.initialMetadata()));
  }

  /**
   * Like {@link #resetHistory(Notebook, Instant)}, but installs the supplied metadata entries (for
   * example none, when demoting a notebook to raw) into the new tip instead of preserving the
   * previous tip's metadata.
   */
  @Transactional
  public NotebookGitBinding resetHistory(
      Notebook notebook, Instant resetTime, List<PortableTreeEntry> metadata) {
    return resetHistory(notebook, findOrCreateBinding(notebook), resetTime, metadata);
  }

  private NotebookGitBinding findOrCreateBinding(Notebook notebook) {
    return notebookGitBindingRepository
        .findByNotebookIdForUpdate(notebook.getId())
        .orElseGet(() -> newBinding(notebook));
  }

  private NotebookGitBinding newBinding(Notebook notebook) {
    NotebookGitBinding binding = new NotebookGitBinding();
    binding.setNotebook(notebook);
    return binding;
  }

  private NotebookGitBinding resetHistory(
      Notebook notebook,
      NotebookGitBinding binding,
      Instant resetTime,
      List<PortableTreeEntry> metadata) {
    try (Repository repository =
        buildRepository(notebook, resetTime, RESET_COMMIT_MESSAGE, metadata)) {
      storeHistory(binding, repository, resetTime);
    }
    return binding;
  }

  private List<PortableTreeEntry> acceptedMetadata(NotebookGitBinding binding) {
    try (NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository opened =
        repositoryStore.open(binding)) {
      return NotebookGitAcceptedTree.metadataEntries(opened.repository(), opened.head());
    }
  }

  private Repository buildRepository(
      Notebook notebook,
      Instant commitTime,
      String message,
      List<PortableTreeEntry> acceptedMetadata) {
    return NotebookGitCommitBuilder.build(
        treeEncoder.fullTree(notebook, acceptedMetadata),
        SYSTEM_AUTHOR_NAME,
        SYSTEM_AUTHOR_EMAIL,
        message,
        commitTime);
  }

  private void storeHistory(NotebookGitBinding binding, Repository repository, Instant time) {
    Timestamp timestamp = Timestamp.from(time);
    if (binding.getCreatedAt() == null) {
      binding.setCreatedAt(timestamp);
    }
    try {
      repositoryStore.store(
          binding,
          repository,
          repository.exactRef(Constants.R_HEADS + "main").getObjectId(),
          timestamp);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
