package com.odde.doughnut.services;

import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.services.index.IndexScope;
import com.odde.doughnut.services.index.ScopedIndexNoteService;
import org.springframework.stereotype.Service;

@Service
public class FolderService {

  private final FolderRepository folderRepository;
  private final ScopedIndexNoteService scopedIndexNoteService;

  public FolderService(
      FolderRepository folderRepository, ScopedIndexNoteService scopedIndexNoteService) {
    this.folderRepository = folderRepository;
    this.scopedIndexNoteService = scopedIndexNoteService;
  }

  /**
   * Sets {@link com.odde.doughnut.entities.Folder#getIndexNote()} from notes titled {@code index}
   * in that folder, or clears it when none exist.
   */
  public void reconcileFolderIndexNotePointer(Integer folderId) {
    if (folderId == null) {
      return;
    }
    folderRepository
        .findById(folderId)
        .ifPresent(
            folder ->
                scopedIndexNoteService.reconcileDesignatedIndexPointer(
                    new IndexScope.FolderIndex(folder)));
  }
}
