package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Locks the notebook Git binding, freezes amendment eligibility for the current tip, and returns
 * that tip's bundle bytes in a write transaction that completes before response delivery.
 */
@Service
public class NotebookGitBundleDownloadService {
  private final NotebookGitBindingRepository bindingRepository;

  public NotebookGitBundleDownloadService(NotebookGitBindingRepository bindingRepository) {
    this.bindingRepository = bindingRepository;
  }

  @Transactional
  public byte[] selectAndFreeze(Integer notebookId) {
    NotebookGitBinding binding =
        bindingRepository
            .findByNotebookIdForUpdate(notebookId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notebook has no Git binding."));
    binding.setAmendmentHead(null);
    binding.setAmendmentNoteId(null);
    binding.setAmendmentLastChangedAt(null);
    return binding.getBundleBytes();
  }
}
