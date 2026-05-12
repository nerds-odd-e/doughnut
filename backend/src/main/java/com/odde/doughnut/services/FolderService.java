package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.index.IndexScope;
import com.odde.doughnut.services.index.ScopedIndexNoteService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FolderService {

  private final FolderRepository folderRepository;
  private final ScopedIndexNoteService scopedIndexNoteService;
  private final EntityPersister entityPersister;

  public FolderService(
      FolderRepository folderRepository,
      ScopedIndexNoteService scopedIndexNoteService,
      EntityPersister entityPersister) {
    this.folderRepository = folderRepository;
    this.scopedIndexNoteService = scopedIndexNoteService;
    this.entityPersister = entityPersister;
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

  public Optional<Note> findOptionalIndexNote(Folder folder) {
    return scopedIndexNoteService.findDesignatedIndexNote(new IndexScope.FolderIndex(folder));
  }
}
