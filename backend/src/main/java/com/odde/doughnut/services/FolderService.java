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

  /**
   * Transition bridge (10.14–10.16): when the designated folder index note's content is updated,
   * mirror it to {@link Folder#getIndexContent()} so the container field stays current until 10.16
   * replaces note-based saves with direct container saves.
   */
  public void syncFolderIndexContentIfDesignated(Note note) {
    Folder folder = note.getFolder();
    if (folder == null || folder.getId() == null) {
      return;
    }
    folderRepository
        .findById(folder.getId())
        .ifPresent(
            f -> {
              Note designatedIndex = f.getIndexNote();
              if (designatedIndex != null
                  && designatedIndex.getId() != null
                  && designatedIndex.getId().equals(note.getId())) {
                f.setIndexContent(note.getContent());
                entityPersister.merge(f);
              }
            });
  }
}
