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
 * Starts and resets one notebook's accepted Git history: builds the notebook's canonical
 * Portable-tree snapshot as of a given commit time, commits it as a single parentless root commit
 * under the Donut system identity, and persists the accepted binding.
 *
 * <p>{@code NotebookService} calls {@link #startHistory} at creation time so every notebook starts
 * Git-backed from an empty content tree with the initial LFS {@code .gitattributes}; {@link
 * #resetHistory} writes the same kind of root commit over a binding a notebook already has,
 * preserving that binding's accepted metadata (or, for a notebook without one, creating it as at
 * creation time). The caller supplies the commit time, and a tree failure propagates before a
 * binding is persisted.
 */
@Service
public class NotebookGitHistoryService {

  static final String CREATION_COMMIT_MESSAGE = "Create notebook";

  static final String RESET_COMMIT_MESSAGE = "Reset: restart Git history from the current notebook";

  private final NotebookGitTreeEncoder treeEncoder;
  private final NotebookGitBindingRepository notebookGitBindingRepository;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public NotebookGitHistoryService(
      NotebookGitTreeEncoder treeEncoder,
      NotebookGitBindingRepository notebookGitBindingRepository,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.treeEncoder = treeEncoder;
    this.notebookGitBindingRepository = notebookGitBindingRepository;
    this.repositoryStore = repositoryStore;
  }

  public NotebookGitBinding startHistory(Notebook notebook, Instant creationTime) {
    return writeRootCommit(
        notebook,
        newBinding(notebook),
        creationTime,
        CREATION_COMMIT_MESSAGE,
        NotebookGitAttributes.initialMetadata());
  }

  /**
   * Restarts {@code notebook}'s accepted Git history: one parentless commit of the notebook's
   * current content replaces whatever the binding held, so the notebook can be cloned and published
   * again whatever state its history was in. Accepted Git metadata such as {@code .gitattributes}
   * is preserved exactly rather than regenerated from defaults; a notebook without a binding gets a
   * new binding with the initial LFS {@code .gitattributes}, as at creation.
   */
  @Transactional
  public NotebookGitBinding resetHistory(Notebook notebook, Instant resetTime) {
    return notebookGitBindingRepository
        .findByNotebookIdForUpdate(notebook.getId())
        .map(
            binding ->
                writeRootCommit(
                    notebook, binding, resetTime, RESET_COMMIT_MESSAGE, acceptedMetadata(binding)))
        .orElseGet(
            () ->
                writeRootCommit(
                    notebook,
                    newBinding(notebook),
                    resetTime,
                    RESET_COMMIT_MESSAGE,
                    NotebookGitAttributes.initialMetadata()));
  }

  private NotebookGitBinding newBinding(Notebook notebook) {
    NotebookGitBinding binding = new NotebookGitBinding();
    binding.setNotebook(notebook);
    return binding;
  }

  private NotebookGitBinding writeRootCommit(
      Notebook notebook,
      NotebookGitBinding binding,
      Instant commitTime,
      String message,
      List<PortableTreeEntry> metadata) {
    try (Repository repository =
        NotebookGitCommitBuilder.build(
            treeEncoder.fullTree(notebook, metadata),
            NotebookGitCommitBuilder.SYSTEM_AUTHOR_NAME,
            NotebookGitCommitBuilder.SYSTEM_AUTHOR_EMAIL,
            message,
            commitTime)) {
      storeHistory(binding, repository, commitTime);
    }
    return binding;
  }

  private List<PortableTreeEntry> acceptedMetadata(NotebookGitBinding binding) {
    try (NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository opened =
        repositoryStore.open(binding)) {
      return NotebookGitAcceptedTree.metadataEntries(opened.repository(), opened.head());
    }
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
