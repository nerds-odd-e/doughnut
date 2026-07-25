package com.odde.doughnut.services.health;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EmptyFolderBulkPurge {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;

  public EmptyFolderBulkPurge(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      EntityPersister entityPersister) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
  }

  public void apply(Notebook notebook) {
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    Set<Integer> occupiedFolderIds =
        new HashSet<>(noteRepository.findLiveNoteFolderIdsByNotebookId(notebook.getId()));

    List<Folder> deletable =
        FolderSubtreeLiveNotes.cascadeSafeFullyEmptyFolders(folders, occupiedFolderIds);
    for (Folder folder : deletable) {
      detachNotesFromFolder(folder);
      entityPersister.remove(folder);
    }
  }

  private void detachNotesFromFolder(Folder folder) {
    List<Note> notes =
        entityPersister
            .createQuery("SELECT n FROM Note n WHERE n.folder.id = :folderId", Note.class)
            .setParameter("folderId", folder.getId())
            .getResultList();
    for (Note note : notes) {
      note.setFolder(null);
      entityPersister.merge(note);
    }
    entityPersister.flush();
  }
}
