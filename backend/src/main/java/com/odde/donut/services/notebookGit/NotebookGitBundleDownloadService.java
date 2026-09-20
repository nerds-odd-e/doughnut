package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notebook has no Git binding."));
    return repositoryStore.downloadableBundle(binding);
  }
}
