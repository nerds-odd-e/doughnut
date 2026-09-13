package com.odde.donut.services;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderConstructionService {

  private final NoteRepository noteRepository;
  private final FolderRepository folderRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  public FolderConstructionService(
      NoteRepository noteRepository,
      FolderRepository folderRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.noteRepository = noteRepository;
    this.folderRepository = folderRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
  }

  public Folder createFolder(Notebook notebook, FolderCreationRequest request) {
    String name = request.getName();

    Folder parentFolder = null;
    Integer underFolderId = request.getUnderFolderId();
    if (underFolderId != null) {
      Folder parent =
          folderRepository
              .findById(underFolderId)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.NOT_FOUND, "Parent folder not found."));
      if (!parent.getNotebook().getId().equals(notebook.getId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
      }
      parentFolder = parent;
    } else {
      Integer underNoteId = request.getUnderNoteId();
      if (underNoteId != null) {
        Note contextNote =
            noteRepository
                .findById(underNoteId)
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Context note not found."));
        if (!contextNote.getNotebook().getId().equals(notebook.getId())) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not in notebook.");
        }
        if (contextNote.getDeletedAt() != null) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Context note not found.");
        }
        parentFolder = contextNote.getFolder();
      }
    }

    return createFolder(notebook, parentFolder, new DisplayName(name));
  }

  public Folder ensureTrashParentFor(Note note) {
    Notebook notebook = note.getNotebook();
    Folder parent =
        folderRepository.findRootFoldersByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(Folder::isTrashed)
            .findFirst()
            .orElseGet(() -> createFolder(notebook, null, new DisplayName("_trash")));
    for (Folder sourceFolder : FolderTrailSegments.fromRootToContainingFolder(note)) {
      parent = findOrCreateFolder(notebook, parent, new DisplayName(sourceFolder.getName()));
    }
    return parent;
  }

  public Folder createFolder(Notebook notebook, Folder parentFolder, DisplayName displayName) {
    if (parentFolder != null && !parentFolder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }

    Integer parentFolderId = parentFolder == null ? null : parentFolder.getId();
    folderSiblingNameValidation.requireNoConflictingSibling(
        notebook.getId(), parentFolderId, displayName);

    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    Folder folder = new Folder();
    folder.setNotebook(notebook);
    folder.setParentFolder(parentFolder);
    folder.setName(displayName);
    folder.setCreatedAt(now);
    folder.setUpdatedAt(now);
    entityPersister.save(folder);
    return folder;
  }

  private Folder findOrCreateFolder(
      Notebook notebook, Folder parentFolder, DisplayName displayName) {
    Integer parentFolderId = parentFolder == null ? null : parentFolder.getId();
    return folderSiblingNameValidation
        .findConflictingSibling(notebook.getId(), parentFolderId, displayName, Set.of())
        .orElseGet(() -> createFolder(notebook, parentFolder, displayName));
  }
}
