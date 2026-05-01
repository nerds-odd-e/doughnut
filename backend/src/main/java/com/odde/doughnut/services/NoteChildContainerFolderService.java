package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoteChildContainerFolderService {

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  public NoteChildContainerFolderService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
  }

  public Folder resolveForParent(Note parentNote) {
    return noteRepository
        .findFirstByParent_IdAndFolderIsNotNullAndDeletedAtIsNullOrderByIdAsc(parentNote.getId())
        .map(Note::getFolder)
        .orElseGet(() -> findOrCreateChildContainer(parentNote));
  }

  private Folder findOrCreateChildContainer(Note parentNote) {
    Folder parentFolderContext =
        parentNote.getParent() != null
            ? findExistingChildContainerForAncestor(parentNote.getParent())
            : parentNote.getFolder();
    Integer parentFolderId = parentFolderContext == null ? null : parentFolderContext.getId();
    List<Folder> candidates =
        folderRepository.findCandidateChildContainers(
            parentNote.getNotebook().getId(), parentFolderId, folderNameForNote(parentNote));
    if (!candidates.isEmpty()) {
      return candidates.getFirst();
    }
    return createFolder(parentNote, parentFolderContext);
  }

  private Folder findExistingChildContainerForAncestor(Note note) {
    if (note == null) {
      return null;
    }
    Folder ancestorParentFolder = findExistingChildContainerForAncestor(note.getParent());
    Integer parentFolderId = ancestorParentFolder == null ? null : ancestorParentFolder.getId();
    List<Folder> found =
        folderRepository.findCandidateChildContainers(
            note.getNotebook().getId(), parentFolderId, folderNameForNote(note));
    if (found.isEmpty()) {
      return null;
    }
    return found.getFirst();
  }

  private Folder createFolder(Note parentNote, Folder parentFolderContext) {
    Folder folder = new Folder();
    folder.setNotebook(parentNote.getNotebook());
    folder.setParentFolder(parentFolderContext);
    folder.setName(folderNameForNote(parentNote));
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    folder.setCreatedAt(now);
    folder.setUpdatedAt(now);
    entityPersister.save(folder);
    return folder;
  }

  private static String folderNameForNote(Note note) {
    String title = note.getTitle();
    if (title == null || title.isEmpty()) {
      return " ";
    }
    return title;
  }
}
