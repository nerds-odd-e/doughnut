package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.FolderListing;
import com.odde.donut.controllers.dto.FolderRealm;
import com.odde.donut.controllers.dto.NoteTopology;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.NotebookCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Read-only folder listing, page, and index HTTP for notebooks. */
abstract class NotebookFolderQuerySupport {
  private final AuthorizationService authorizationService;
  private final NoteService noteService;
  private final FolderRepository folderRepository;
  private final NotebookCatalogService notebookCatalogService;

  NotebookFolderQuerySupport(
      AuthorizationService authorizationService,
      NoteService noteService,
      FolderRepository folderRepository,
      NotebookCatalogService notebookCatalogService) {
    this.authorizationService = authorizationService;
    this.noteService = noteService;
    this.folderRepository = folderRepository;
    this.notebookCatalogService = notebookCatalogService;
  }

  @Operation(
      summary = "List notes and folders at notebook root or under a parent folder",
      description =
          "Without parent: notes with no folder assignment and top-level folders (notebook root"
              + " scope). With parent: notes assigned to that folder and its immediate child"
              + " folders. The parent folder must belong to the notebook.")
  @GetMapping("/{notebook}/folder-listing")
  public FolderListing listNotebookFolderListing(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @RequestParam(value = "parent", required = false) @Schema(type = "integer")
          Integer parentFolderId)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    if (parentFolderId == null) {
      List<NoteTopology> noteTopologies =
          noteService.findNotebookRootNotes(notebook.getId()).stream()
              .map(Note::getNoteTopology)
              .toList();
      List<Folder> folders =
          folderRepository.findRootFoldersByNotebookIdOrderByIdAsc(notebook.getId()).stream()
              .toList();
      return new FolderListing(noteTopologies, folders);
    }
    Folder folder =
        folderRepository
            .findById(parentFolderId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found."));
    folder.requireInNotebook(notebook);
    List<NoteTopology> noteTopologies =
        noteService.findNotesInFolderScope(folder.getId()).stream()
            .map(Note::getNoteTopology)
            .toList();
    List<Folder> childFolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId()).stream()
            .toList();
    return new FolderListing(noteTopologies, childFolders);
  }

  @Operation(
      summary = "Get folder page payload",
      description =
          "Notebook chrome, folder metadata, parent folder id when nested, and optional folder"
              + " readme content when present.")
  @GetMapping("/{notebook}/folders/{folder}")
  public FolderRealm getFolderPage(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    folder.requireInNotebook(notebook);
    User user = authorizationService.getCurrentUser();
    return notebookCatalogService.folderRealmFor(notebook, folder, user);
  }

  @Operation(
      description =
          "Folder rows (including parentFolderId) for building folder trees and paths. Ordered by"
              + " id.")
  @GetMapping("/{notebook}/folders/index")
  public List<Folder> listNotebookFolderIndex(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    return folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
  }

  protected FolderRepository folderRepository() {
    return folderRepository;
  }

  protected NotebookCatalogService notebookCatalogService() {
    return notebookCatalogService;
  }

  protected AuthorizationService authorizationService() {
    return authorizationService;
  }
}
