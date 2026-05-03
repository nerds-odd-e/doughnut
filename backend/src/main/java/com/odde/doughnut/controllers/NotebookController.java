package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.FolderCreationRequest;
import com.odde.doughnut.controllers.dto.FolderListing;
import com.odde.doughnut.controllers.dto.FolderTrailSegment;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.NotebookClientView;
import com.odde.doughnut.controllers.dto.NotebookUpdateRequest;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.controllers.dto.UpdateAiAssistantRequest;
import com.odde.doughnut.controllers.dto.UpdateNotebookGroupRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NotebookGroupRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.BazaarService;
import com.odde.doughnut.services.FolderConstructionService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.NotebookCatalogService;
import com.odde.doughnut.services.NotebookGroupService;
import com.odde.doughnut.services.NotebookIndexingService;
import com.odde.doughnut.services.NotebookService;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookController {
  private final EntityPersister entityPersister;
  private final NotebookService notebookService;

  private final TestabilitySettings testabilitySettings;

  private final NotebookIndexingService notebookIndexingService;
  private final BazaarService bazaarService;
  private final AuthorizationService authorizationService;
  private final NotebookGroupRepository notebookGroupRepository;
  private final NotebookGroupService notebookGroupService;
  private final NotebookRepository notebookRepository;
  private final FolderRepository folderRepository;
  private final NotebookCatalogService notebookCatalogService;
  private final NoteService noteService;
  private final NoteConstructionService noteConstructionService;
  private final WikidataService wikidataService;
  private final FolderConstructionService folderConstructionService;

  public NotebookController(
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NotebookIndexingService notebookIndexingService,
      BazaarService bazaarService,
      AuthorizationService authorizationService,
      NotebookService notebookService,
      NotebookGroupRepository notebookGroupRepository,
      NotebookGroupService notebookGroupService,
      NotebookRepository notebookRepository,
      FolderRepository folderRepository,
      NotebookCatalogService notebookCatalogService,
      NoteService noteService,
      NoteConstructionService noteConstructionService,
      WikidataService wikidataService,
      FolderConstructionService folderConstructionService) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.notebookIndexingService = notebookIndexingService;
    this.bazaarService = bazaarService;
    this.authorizationService = authorizationService;
    this.notebookService = notebookService;
    this.notebookGroupRepository = notebookGroupRepository;
    this.notebookGroupService = notebookGroupService;
    this.notebookRepository = notebookRepository;
    this.folderRepository = folderRepository;
    this.notebookCatalogService = notebookCatalogService;
    this.noteService = noteService;
    this.noteConstructionService = noteConstructionService;
    this.wikidataService = wikidataService;
    this.folderConstructionService = folderConstructionService;
  }

  @GetMapping("")
  public NotebooksViewedByUser myNotebooks() {
    authorizationService.assertLoggedIn();

    User user = authorizationService.getCurrentUser();
    var ownership = user.getOwnership();
    List<NotebookGroup> groups = notebookGroupRepository.findByOwnership_Id(ownership.getId());
    List<Notebook> notebooks =
        notebookRepository.findByOwnership_IdAndDeletedAtIsNull(ownership.getId());
    List<Subscription> subscriptions = user.getSubscriptions();
    return notebookCatalogService.buildView(user, notebooks, groups, subscriptions);
  }

  @PostMapping({"/create"})
  @Transactional
  public NotebookClientView createNotebook(@Valid @RequestBody NoteCreationDTO noteCreation) {
    authorizationService.assertLoggedIn();
    User userEntity = authorizationService.getCurrentUser();
    Notebook notebook =
        notebookService.createNotebookForOwnership(
            userEntity.getOwnership(),
            userEntity,
            testabilitySettings.getCurrentUTCTimestamp(),
            noteCreation.getNewTitle(),
            noteCreation.getDescription());
    return notebookCatalogService.clientViewFor(notebook, userEntity);
  }

  @PostMapping(value = "/{notebook}/create-note")
  @Transactional
  public NoteRealm createNoteAtNotebookRoot(
      @PathVariable @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    authorizationService.assertAuthorization(notebook);
    User user = authorizationService.getCurrentUser();
    return noteConstructionService.createRootNoteWithWikidataService(
        notebook,
        noteCreation,
        user,
        wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId));
  }

  @Operation(
      summary = "Create a folder",
      description =
          "Creates a folder at notebook root when no parent is specified; as a child of"
              + " underFolderId when set; otherwise nested under the context note's folder when"
              + " underNoteId is set (underFolderId takes precedence when both are set).")
  @PostMapping("/{notebook}/folders")
  @Transactional
  public FolderTrailSegment createFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody FolderCreationRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return folderConstructionService.createFolder(notebook, request);
  }

  @PostMapping(value = "/{notebook}")
  @Transactional
  public Notebook updateNotebook(
      @PathVariable @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody NotebookUpdateRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    notebook.getNotebookSettings().update(request.getNotebookSettings());
    if (request.getDescription() != null) {
      notebook.setDescription(request.getDescription().isBlank() ? null : request.getDescription());
    }
    if (request.getName() != null) {
      String trimmedName = request.getName().trim();
      if (trimmedName.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notebook name cannot be empty");
      }
      notebook.setName(trimmedName);
    }
    entityPersister.save(notebook);
    return notebook;
  }

  @GetMapping(value = "/{notebook}")
  public NotebookClientView get(@PathVariable @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    User user = authorizationService.getCurrentUser();
    return notebookCatalogService.clientViewFor(notebook, user);
  }

  @PostMapping(value = "/{notebook}/share")
  @Transactional
  public Notebook shareNotebook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    bazaarService.shareNotebook(notebook);
    return notebook;
  }

  @PatchMapping("/{notebook}/notebook-group")
  @Transactional
  public Notebook updateNotebookGroup(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody UpdateNotebookGroupRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    User user = authorizationService.getCurrentUser();
    if (request.getNotebookGroupId() == null) {
      notebookGroupService.clearNotebookGroup(user, notebook);
    } else {
      NotebookGroup group =
          notebookGroupRepository
              .findById(request.getNotebookGroupId())
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      notebookGroupService.assignNotebookToGroup(user, notebook, group);
    }
    return notebook;
  }

  @PatchMapping(value = "/{notebook}/move-to-circle/{circle}")
  @Transactional
  public Notebook moveToCircle(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("circle") @Schema(type = "integer") Circle circle)
      throws UnexpectedNoAccessRightException {
    if (notebook.getCreatorEntity().getId() != authorizationService.getCurrentUser().getId()) {
      throw new UnexpectedNoAccessRightException();
    }
    notebook.setOwnership(circle.getOwnership());
    entityPersister.save(notebook);
    return notebook;
  }

  @PatchMapping("/{notebook}/ai-assistant")
  @Transactional
  public NotebookAiAssistant updateAiAssistant(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody UpdateAiAssistantRequest request)
      throws UnexpectedNoAccessRightException {

    authorizationService.assertAuthorization(notebook);

    NotebookAiAssistant assistant = notebookService.findByNotebookId(notebook.getId());
    if (assistant == null) {
      assistant = new NotebookAiAssistant();
      assistant.setNotebook(notebook);
      assistant.setCreatedAt(testabilitySettings.getCurrentUTCTimestamp());
    }

    assistant.setAdditionalInstructionsToAi(request.getAdditionalInstructions());
    assistant.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());

    return notebookService.save(assistant);
  }

  @GetMapping("/{notebook}/ai-assistant")
  public NotebookAiAssistant getAiAssistant(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {

    authorizationService.assertAuthorization(notebook);
    return notebookService.findByNotebookId(notebook.getId());
  }

  @Operation(
      summary = "List notebook root: notes without a folder and top-level folders",
      description =
          "Notes are those in the notebook with no folder assignment (notebook root scope), not"
              + " filtered by legacy parent. Folders are notebook root folders (no parent folder).")
  @GetMapping("/{notebook}/root-notes")
  public FolderListing listNotebookRootNotes(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    List<NoteTopology> noteTopologies =
        noteService.findNotebookRootNotes(notebook.getId()).stream()
            .map(Note::getNoteTopology)
            .toList();
    List<FolderTrailSegment> folders =
        folderRepository.findRootFoldersByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .map(FolderTrailSegment::from)
            .toList();
    return new FolderListing(noteTopologies, folders);
  }

  @Operation(
      summary = "List folder scope: notes in folder and direct child folders",
      description =
          "Notes are those assigned to the folder. Folders are immediate children of the given"
              + " folder.")
  @GetMapping("/{notebook}/folders/{folder}/listing")
  public FolderListing listFolderListing(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
    List<NoteTopology> noteTopologies =
        noteService.findNotesInFolderScope(folder.getId()).stream()
            .map(Note::getNoteTopology)
            .toList();
    List<FolderTrailSegment> childFolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId()).stream()
            .map(FolderTrailSegment::from)
            .toList();
    return new FolderListing(noteTopologies, childFolders);
  }

  @PostMapping("/{notebook}/update-index")
  @Transactional
  public void updateNotebookIndex(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    notebookIndexingService.updateNotebookIndex(notebook);
  }

  @PostMapping("/{notebook}/reset-index")
  @Transactional
  public void resetNotebookIndex(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    notebookIndexingService.resetNotebookIndex(notebook);
  }
}
