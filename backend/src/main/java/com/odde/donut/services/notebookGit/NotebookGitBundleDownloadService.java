package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Selects the accepted bundle under the notebook Git writer lock. */
@Service
public class NotebookGitBundleDownloadService {
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public NotebookGitBundleDownloadService(
      NotebookGitBindingRepository bindingRepository,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.bindingRepository = bindingRepository;
    this.repositoryStore = repositoryStore;
  }

  @Transactional
  public byte[] select(Integer notebookId) {
    NotebookGitBinding binding =
        bindingRepository
            .findByNotebookIdForUpdate(notebookId)
            .orElseThrow(NotebookGitBindingMissing::refusal);
    return repositoryStore.downloadableBundle(binding);
  }
}
