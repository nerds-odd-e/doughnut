package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.controllers.dto.FolderRealm;
import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.FolderRelocationService;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.NotebookCatalogService;
import com.odde.donut.services.notebookGit.AcceptedWebChangeService;
import com.odde.donut.services.notebookGit.WebFolderCreationService;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookFolderController extends NotebookFolderQuerySupport {
  private final WebFolderCreationService webFolderCreationService;
  private final FolderRelocationService folderRelocationService;
  private final NotebookRepository notebookRepository;
  private final EntityPersister entityPersister;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final TestabilitySettings testabilitySettings;

  NotebookFolderController(
      AuthorizationService authorizationService,
      NoteService noteService,
      FolderRepository folderRepository,
      NotebookCatalogService notebookCatalogService,
      WebFolderCreationService webFolderCreationService,
      FolderRelocationService folderRelocationService,
      NotebookRepository notebookRepository,
      EntityPersister entityPersister,
      AcceptedWebChangeService acceptedWebChangeService,
      TestabilitySettings testabilitySettings) {
    super(authorizationService, noteService, folderRepository, notebookCatalogService);
    this.webFolderCreationService = webFolderCreationService;
    this.folderRelocationService = folderRelocationService;
    this.notebookRepository = notebookRepository;
    this.entityPersister = entityPersister;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.testabilitySettings = testabilitySettings;
  }

  @Operation(
      summary = "Create a folder",
      description =
          "Creates a folder at notebook root when no parent is specified; as a child of"
              + " underFolderId when set; otherwise nested under the context note's folder when"
              + " underNoteId is set (underFolderId takes precedence when both are set).")
  @PostMapping("/{notebook}/folders")
  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public Folder createFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody FolderCreationRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService().assertAuthorization(notebook);
    return webFolderCreationService.createFolder(notebook, request);
  }

  @Operation(
      summary = "Move a folder",
      description =
          "Reparents the folder within the same notebook, or moves the folder subtree to another"
              + " notebook when destinationNotebookId is set. Notes keep their folderId"
              + " pointing at the same folder rows; descendant folders stay under the moved"
              + " subtree. A same-name folder at the destination returns 409 with"
              + " FOLDER_NAME_CONFLICT unless merge=true, in which case the source subtree is"
              + " merged into the existing folder (including cross-notebook moves).")
  @PostMapping("/{notebook}/folders/{folder}/move")
  public Folder moveFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder,
      @Valid @RequestBody(required = false) FolderMoveRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService().assertAuthorization(notebook);
    Notebook destinationNotebook = resolveDestinationNotebookForFolderMove(request);
    User user = authorizationService().getCurrentUser();
    if (destinationNotebook != null && !destinationNotebook.getId().equals(notebook.getId())) {
      return folderRelocationService.moveFolder(
          notebook, folder, request, destinationNotebook, user);
    }
    return folderRelocationService.moveFolderWithinNotebook(notebook, folder, request, user);
  }

  @Operation(
      summary = "Trash a folder",
      description =
          "Moves an active folder subtree beneath _trash while mirroring its original ancestor"
              + " path. The subtree and authored content are retained for ordinary Move recovery.")
  @PostMapping("/{notebook}/folders/{folder}/trash")
  public Folder trashFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder)
      throws UnexpectedNoAccessRightException {
    authorizationService().assertAuthorization(notebook);
    return folderRelocationService.trashFolderWithinNotebook(notebook, folder);
  }

  @Operation(
      summary = "Permanently delete a trashed folder",
      description =
          "Removes a folder that is in trash with its README, every nested folder, every note"
              + " inside the subtree and each note's dependent data, in one accepted change."
              + " A folder that is not in trash is refused with 400.")
  @PostMapping("/{notebook}/folders/{folder}/permanently-delete")
  public void permanentlyDeleteFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder)
      throws UnexpectedNoAccessRightException {
    authorizationService().assertAuthorization(notebook);
    folderRelocationService.permanentlyDeleteFolderWithinNotebook(notebook, folder);
  }

  @Operation(
      summary = "Rename a folder",
      description =
          "Changes the folder display name under its current parent. Sibling name conflicts are"
              + " rejected.")
  @PatchMapping("/{notebook}/folders/{folder}")
  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public Folder renameFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder,
      @Valid @RequestBody FolderRenameRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService().assertAuthorization(notebook);
    folder.requireInNotebook(notebook);
    User user = authorizationService().getCurrentUser();
    return folderRelocationService.renameFolder(notebook, folder, request, user);
  }

  @Operation(
      summary = "Dissolve a folder",
      description =
          "Removes the folder row. Direct notes and subfolders are promoted to the dissolved"
              + " folder's parent (or notebook root). Deeper descendants stay under the promoted"
              + " subfolder. When merge=true, clashing promoted subfolders are merged into the"
              + " existing same-name sibling instead of returning 409.")
  @DeleteMapping("/{notebook}/folders/{folder}")
  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public void dissolveFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder,
      @RequestParam(name = "merge", defaultValue = "false") boolean merge)
      throws UnexpectedNoAccessRightException {
    authorizationService().assertAuthorization(notebook);
    User user = authorizationService().getCurrentUser();
    folderRelocationService.dissolveFolder(notebook, folder, merge, user);
  }

  @Operation(
      summary = "Update folder readme content directly",
      description =
          "Saves the given markdown (with optional YAML frontmatter) as the folder container's"
              + " readmeContent field. Blank content clears the field.")
  @PatchMapping("/{notebook}/folders/{folder}/readme-content")
  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public FolderRealm updateFolderReadmeContent(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder,
      @RequestBody NoteUpdateContentDTO dto)
      throws UnexpectedNoAccessRightException {
    authorizationService().assertAuthorization(notebook);
    folder.requireInNotebook(notebook);
    String content = dto.getContent();
    if (content != null && !content.isBlank()) {
      content = AuthoredNoteContent.prepareContentForSave(content);
    }
    String readme = content == null || content.isBlank() ? null : content;
    return acceptedWebChangeService.apply(
        notebook.getId(),
        () -> {
          Folder liveFolder = folderRepository().findById(folder.getId()).orElseThrow();
          liveFolder.setReadmeContent(readme);
          entityPersister.save(liveFolder);
          Notebook liveNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
          User user = authorizationService().getCurrentUser();
          return notebookCatalogService().folderRealmFor(liveNotebook, liveFolder, user);
        },
        realm -> "Edit folder README: " + folder.getName(),
        testabilitySettings.getCurrentUTCTimestamp());
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
    authorizationService().assertAuthorization(destinationNotebook);
    return destinationNotebook;
  }
}
