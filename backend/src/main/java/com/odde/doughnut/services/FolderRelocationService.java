package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.controllers.dto.FolderRenameRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderRelocationService {

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NoteTitlePlacementRules noteTitlePlacementRules;

  public FolderRelocationService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NoteTitlePlacementRules noteTitlePlacementRules) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteTitlePlacementRules = noteTitlePlacementRules;
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

    FolderMoveDestinationRules.requireNotMovingIntoSelfOrDescendant(folder, newParent);

    Integer destParentId = newParent == null ? null : newParent.getId();
    Optional<Folder> existingSibling =
        folderRepository
            .findCandidateChildContainers(notebook.getId(), destParentId, folder.getName())
            .stream()
            .filter(f -> !f.getId().equals(folder.getId()))
            .findFirst();

    if (existingSibling.isPresent()) {
      if (request != null && request.isMerge()) {
        mergeFolderInto(folder, existingSibling.get());
        return existingSibling.get();
      }
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

  private void mergeFolderInto(Folder source, Folder target) {
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();

    List<Folder> srcSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(source.getId());
    for (Folder srcChild : srcSubfolders) {
      Optional<Folder> tgtChild =
          folderRepository
              .findCandidateChildContainers(
                  target.getNotebook().getId(), target.getId(), srcChild.getName())
              .stream()
              .findFirst();
      if (tgtChild.isPresent()) {
        mergeFolderInto(srcChild, tgtChild.get());
      } else {
        srcChild.setParentFolder(target);
        srcChild.setUpdatedAt(now);
        entityPersister.merge(srcChild);
      }
    }

    List<Note> srcNotes = noteRepository.findNotesInFolderOrderByIdAsc(source.getId());
    for (Note note : srcNotes) {
      noteTitlePlacementRules.requireNoSoftDeletedTitleAt(
          target.getNotebook(), target, note.getTitle());
      note.setFolder(target);
      entityPersister.merge(note);
    }

    target.setUpdatedAt(now);
    entityPersister.merge(target);
    entityPersister.flush();
    entityPersister.remove(source);
  }

  public Folder renameFolder(Notebook notebook, Folder folder, FolderRenameRequest request) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
    String trimmedName = request.getName().trim();
    if (trimmedName.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder name must not be blank.");
    }
    if (trimmedName.equals(folder.getName())) {
      return folder;
    }
    Integer parentFolderId =
        folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
    folderSiblingNameValidation.requireNoConflictingSibling(
        notebook.getId(), parentFolderId, trimmedName, folder.getId());
    folder.setName(trimmedName);
    folder.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    return folder;
  }

  public void dissolveFolder(Notebook notebook, Folder folder, boolean merge) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }

    Folder destination = folder.getParentFolder();
    Integer destinationId = destination == null ? null : destination.getId();

    List<Folder> directSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId());

    for (Folder child : directSubfolders) {
      Optional<Folder> existingSibling =
          folderRepository
              .findCandidateChildContainers(notebook.getId(), destinationId, child.getName())
              .stream()
              .filter(f -> !f.getId().equals(folder.getId()))
              .findFirst();
      if (existingSibling.isEmpty()) {
        continue;
      }
      if (merge) {
        mergeFolderInto(child, existingSibling.get());
      } else {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "A folder with this name already exists at the destination: " + child.getName());
      }
    }

    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    List<Folder> remainingSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId());
    for (Folder child : remainingSubfolders) {
      child.setParentFolder(destination);
      child.setUpdatedAt(now);
      entityPersister.merge(child);
    }

    List<Note> directNotes = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
    for (Note note : directNotes) {
      noteTitlePlacementRules.requireNoSoftDeletedTitleAt(notebook, destination, note.getTitle());
      note.setFolder(destination);
      entityPersister.merge(note);
    }

    entityPersister.flush();
    entityPersister.remove(folder);
  }
}
