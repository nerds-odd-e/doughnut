package com.odde.doughnut.controllers;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.controllers.dto.FolderCreationRequest;
import com.odde.doughnut.controllers.dto.FolderListing;
import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.controllers.dto.FolderRealm;
import com.odde.doughnut.controllers.dto.FolderRenameRequest;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.NoteUpdateContentDTO;
import com.odde.doughnut.controllers.dto.NotebookCreationRequest;
import com.odde.doughnut.controllers.dto.NotebookRealm;
import com.odde.doughnut.controllers.dto.NotebookUpdateRequest;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
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
import com.odde.doughnut.services.FolderRelocationService;
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
  private final FolderRelocationService folderRelocationService;

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
      FolderConstructionService folderConstructionService,
      FolderRelocationService folderRelocationService) {
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
    this.folderRelocationService = folderRelocationService;
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
  public NotebookRealm createNotebook(@Valid @RequestBody NotebookCreationRequest noteCreation) {
    authorizationService.assertLoggedIn();
    User userEntity = authorizationService.getCurrentUser();
    Notebook notebook =
        notebookService.createNotebookForOwnership(
            userEntity.getOwnership(),
            userEntity,
            testabilitySettings.getCurrentUTCTimestamp(),
            noteCreation.getNewTitle(),
            noteCreation.getDescription());
    return notebookCatalogService.notebookRealmFor(notebook, userEntity);
  }

  @PostMapping(value = "/{notebook}/create-note")
  @Transactional
  public NoteRealm createNoteAtNotebookRoot(
      @PathVariable @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException {
    authorizationService.assertAuthorization(notebook);
    User user = authorizationService.getCurrentUser();
    return noteConstructionService.createRootNoteWithWikidataService(
        notebook,
        noteCreation,
        user,
        wikidataService.wrapWikidataIdWithApi(
            NoteContentMarkdown.wikidataIdScalarFromLeadingFrontmatter(noteCreation.getContent())
                .orElse(null)));
  }

  @Operation(
      summary = "Create a folder",
      description =
          "Creates a folder at notebook root when no parent is specified; as a child of"
              + " underFolderId when set; otherwise nested under the context note's folder when"
              + " underNoteId is set (underFolderId takes precedence when both are set).")
  @PostMapping("/{notebook}/folders")
  @Transactional
  public Folder createFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody FolderCreationRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return folderConstructionService.createFolder(notebook, request);
  }

  @Operation(
      summary = "Move a folder",
      description =
          "Reparents the folder within the same notebook, or moves the folder subtree to another"
              + " notebook when destinationNotebookId is set. Notes keep their folderId"
              + " pointing at the same folder rows; descendant folders stay under the moved"
              + " subtree.")
  @PostMapping("/{notebook}/folders/{folder}/move")
  @Transactional
  public Folder moveFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder,
      @Valid @RequestBody(required = false) FolderMoveRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    Notebook destinationNotebook = resolveDestinationNotebookForFolderMove(request);
    User user = authorizationService.getCurrentUser();
    return folderRelocationService.moveFolder(notebook, folder, request, destinationNotebook, user);
  }

  @Operation(
      summary = "Rename a folder",
      description =
          "Changes the folder display name under its current parent. Sibling name conflicts are"
              + " rejected.")
  @PatchMapping("/{notebook}/folders/{folder}")
  @Transactional
  public Folder renameFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder,
      @Valid @RequestBody FolderRenameRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    assertFolderInNotebook(notebook, folder);
    return folderRelocationService.renameFolder(notebook, folder, request);
  }

  @Operation(
      summary = "Dissolve a folder",
      description =
          "Removes the folder row. Direct notes and subfolders are promoted to the dissolved"
              + " folder's parent (or notebook root). Deeper descendants stay under the promoted"
              + " subfolder. When merge=true, clashing promoted subfolders are merged into the"
              + " existing same-name sibling instead of returning 409.")
  @DeleteMapping("/{notebook}/folders/{folder}")
  @Transactional
  public void dissolveFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder,
      @RequestParam(name = "merge", defaultValue = "false") boolean merge)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    folderRelocationService.dissolveFolder(notebook, folder, merge);
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
  public NotebookRealm get(@PathVariable @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    User user = authorizationService.getCurrentUser();
    return notebookCatalogService.notebookRealmFor(notebook, user);
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
    if (notebook.getCreator().getId() != authorizationService.getCurrentUser().getId()) {
      throw new UnexpectedNoAccessRightException();
    }
    notebook.setOwnership(circle.getOwnership());
    entityPersister.save(notebook);
    return notebook;
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
    assertFolderInNotebook(notebook, folder);
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
          "Notebook chrome, folder metadata, parent folder id when nested, and designated folder"
              + " index note id when resolved.")
  @GetMapping("/{notebook}/folders/{folder}")
  public FolderRealm getFolderPage(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    assertFolderInNotebook(notebook, folder);
    User user = authorizationService.getCurrentUser();
    return notebookCatalogService.folderRealmFor(notebook, folder, user);
  }

  @Operation(
      summary = "Update notebook index content directly",
      description =
          "Saves the given markdown (with optional YAML frontmatter) as the notebook container's"
              + " indexContent field. Blank content clears the field.")
  @PatchMapping("/{notebook}/index-content")
  @Transactional
  public NotebookRealm updateNotebookIndexContent(
      @PathVariable @Schema(type = "integer") Notebook notebook,
      @RequestBody NoteUpdateContentDTO dto)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    String content = dto.getContent();
    notebook.setIndexContent(content == null || content.isBlank() ? null : content);
    entityPersister.save(notebook);
    entityPersister.flush();
    User user = authorizationService.getCurrentUser();
    return notebookCatalogService.notebookRealmFor(notebook, user);
  }

  @Operation(
      summary = "Update folder index content directly",
      description =
          "Saves the given markdown (with optional YAML frontmatter) as the folder container's"
              + " indexContent field. Blank content clears the field.")
  @PatchMapping("/{notebook}/folders/{folder}/index-content")
  @Transactional
  public FolderRealm updateFolderIndexContent(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder,
      @RequestBody NoteUpdateContentDTO dto)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    assertFolderInNotebook(notebook, folder);
    String content = dto.getContent();
    folder.setIndexContent(content == null || content.isBlank() ? null : content);
    entityPersister.save(folder);
    entityPersister.flush();
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

  private Notebook resolveDestinationNotebookForFolderMove(FolderMoveRequest request)
      throws UnexpectedNoAccessRightException {
    if (request == null || request.getDestinationNotebookId() == null) {
      return null;
    }
    Notebook destinationNotebook =
        notebookRepository
            .findById(request.getDestinationNotebookId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook not found."));
    authorizationService.assertAuthorization(destinationNotebook);
    return destinationNotebook;
  }

  private void assertFolderInNotebook(Notebook notebook, Folder folder) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
  }
}
