package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderRelocationService {

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  public FolderRelocationService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
  }

  public Folder moveFolder(Notebook notebook, Folder folder, FolderMoveRequest request) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
    Integer newParentFolderId = request != null ? request.getNewParentFolderId() : null;
    Folder newParent = null;
    if (newParentFolderId != null) {
      newParent =
          folderRepository
              .findById(newParentFolderId)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.NOT_FOUND, "Parent folder not found."));
      if (!newParent.getNotebook().getId().equals(notebook.getId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not in notebook.");
      }
    }

    if (newParentFolderId != null && newParentFolderId.equals(folder.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move folder into itself.");
    }
    if (newParent != null && folderIsStrictDescendantOf(folder, newParent)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot move folder into its descendant.");
    }

    Integer destParentId = newParent == null ? null : newParent.getId();
    boolean nameClash =
        folderRepository
            .findCandidateChildContainers(notebook.getId(), destParentId, folder.getName())
            .stream()
            .anyMatch(f -> !f.getId().equals(folder.getId()));
    if (nameClash) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A folder with this name already exists here.");
    }

    folder.setParentFolder(newParent);
    folder.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    return folder;
  }

  public void dissolveFolder(Notebook notebook, Folder folder) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }

    Folder destination = folder.getParentFolder();
    Integer destinationId = destination == null ? null : destination.getId();

    List<Folder> directSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId());
    for (Folder child : directSubfolders) {
      boolean clash =
          folderRepository
              .findCandidateChildContainers(notebook.getId(), destinationId, child.getName())
              .stream()
              .anyMatch(f -> !f.getId().equals(child.getId()) && !f.getId().equals(folder.getId()));
      if (clash) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "A folder with this name already exists at the destination: " + child.getName());
      }
    }

    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    for (Folder child : directSubfolders) {
      child.setParentFolder(destination);
      child.setUpdatedAt(now);
      entityPersister.merge(child);
    }

    List<Note> directNotes = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
    for (Note note : directNotes) {
      note.setFolder(destination);
      entityPersister.merge(note);
    }

    entityPersister.flush();
    entityPersister.remove(folder);
  }

  /** True if {@code possibleDescendant} is {@code ancestor} or strictly under {@code ancestor}. */
  private static boolean folderIsStrictDescendantOf(Folder ancestor, Folder possibleDescendant) {
    Folder x = possibleDescendant;
    while (x != null) {
      if (x.getId().equals(ancestor.getId())) {
        return true;
      }
      x = x.getParentFolder();
    }
    return false;
  }
}
