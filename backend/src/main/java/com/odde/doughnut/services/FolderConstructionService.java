package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FolderCreationRequest;
import com.odde.doughnut.controllers.dto.NotebookRootFolder;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderConstructionService {

  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;
  private final WikiSlugPathService wikiSlugPathService;
  private final TestabilitySettings testabilitySettings;

  public FolderConstructionService(
      NoteRepository noteRepository,
      EntityPersister entityPersister,
      WikiSlugPathService wikiSlugPathService,
      TestabilitySettings testabilitySettings) {
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
    this.wikiSlugPathService = wikiSlugPathService;
    this.testabilitySettings = testabilitySettings;
  }

  public NotebookRootFolder createFolder(Notebook notebook, FolderCreationRequest request) {
    String trimmedName = request.getName().trim();
    if (trimmedName.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder name must not be blank.");
    }

    Folder parentFolder = null;
    Integer underNoteId = request.getUnderNoteId();
    if (underNoteId != null) {
      Note contextNote =
          noteRepository
              .findById(underNoteId)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(HttpStatus.NOT_FOUND, "Context note not found."));
      if (!contextNote.getNotebook().getId().equals(notebook.getId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not in notebook.");
      }
      if (contextNote.getDeletedAt() != null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Context note not found.");
      }
      parentFolder = contextNote.getFolder();
    }

    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    Folder folder = new Folder();
    folder.setNotebook(notebook);
    folder.setParentFolder(parentFolder);
    folder.setName(trimmedName);
    folder.setCreatedAt(now);
    folder.setUpdatedAt(now);
    wikiSlugPathService.assignSlugForNewFolder(folder);
    entityPersister.save(folder);
    return NotebookRootFolder.from(folder);
  }
}
