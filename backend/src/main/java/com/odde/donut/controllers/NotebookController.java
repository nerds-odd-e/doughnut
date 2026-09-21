package com.odde.donut.controllers;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.NotebookCreationRequest;
import com.odde.donut.controllers.dto.NotebookRealm;
import com.odde.donut.controllers.dto.NotebookUpdateRequest;
import com.odde.donut.controllers.dto.NotebooksViewedByUser;
import com.odde.donut.controllers.dto.UpdateNotebookGroupRequest;
import com.odde.donut.entities.*;
import com.odde.donut.entities.repositories.NotebookGroupRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.BazaarService;
import com.odde.donut.services.NotebookCatalogService;
import com.odde.donut.services.NotebookGroupService;
import com.odde.donut.services.NotebookIndexingService;
import com.odde.donut.services.NotebookService;
import com.odde.donut.services.WikidataService;
import com.odde.donut.services.notebookGit.NotebookGitBundleDownloadService;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitProposalImporter;
import com.odde.donut.services.notebookGit.NotebookGitProposalPublisher;
import com.odde.donut.services.notebookGit.WebNoteCreationService;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

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
  private final NotebookCatalogService notebookCatalogService;
  private final WebNoteCreationService webNoteCreationService;
  private final WikidataService wikidataService;
  private final NotebookGitBundleDownloadService notebookGitBundleDownloadService;
  private final NotebookGitProposalPublisher notebookGitProposalPublisher;
  private final NotebookGitCutoverService notebookGitCutoverService;

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
      NotebookCatalogService notebookCatalogService,
      WebNoteCreationService webNoteCreationService,
      WikidataService wikidataService,
      NotebookGitBundleDownloadService notebookGitBundleDownloadService,
      NotebookGitProposalPublisher notebookGitProposalPublisher,
      NotebookGitCutoverService notebookGitCutoverService) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.notebookIndexingService = notebookIndexingService;
    this.bazaarService = bazaarService;
    this.authorizationService = authorizationService;
    this.notebookService = notebookService;
    this.notebookGroupRepository = notebookGroupRepository;
    this.notebookGroupService = notebookGroupService;
    this.notebookRepository = notebookRepository;
    this.notebookCatalogService = notebookCatalogService;
    this.webNoteCreationService = webNoteCreationService;
    this.wikidataService = wikidataService;
    this.notebookGitBundleDownloadService = notebookGitBundleDownloadService;
    this.notebookGitProposalPublisher = notebookGitProposalPublisher;
    this.notebookGitCutoverService = notebookGitCutoverService;
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
  public NotebookRealm createNotebook(@Valid @RequestBody NotebookCreationRequest noteCreation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    User userEntity = authorizationService.getCurrentUser();
    Notebook notebook =
        notebookService.createNotebookForOwnership(
            userEntity.getOwnership(),
            userEntity,
            testabilitySettings.getCurrentUTCTimestamp(),
            noteCreation.getNewTitle(),
            noteCreation.getDescription());
    notebookGroupService.assignNotebookToGroupById(
        userEntity, notebook, noteCreation.getNotebookGroupId());
    return notebookCatalogService.notebookRealmFor(notebook, userEntity);
  }

  @PostMapping(value = "/{notebook}/create-note")
  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public NoteRealm createNoteAtNotebookRoot(
      @PathVariable @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException {
    authorizationService.assertAuthorization(notebook);
    User user = authorizationService.getCurrentUser();
    return webNoteCreationService.createRootNote(
        notebook,
        noteCreation,
        user,
        wikidataService.wrapWikidataIdWithApi(
            NoteContentMarkdown.wikidataIdScalarFromLeadingFrontmatter(noteCreation.getContent())
                .orElse(null)));
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
      notebook.setName(new DisplayName(request.getName()));
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
      notebookGroupService.assignNotebookToGroupById(user, notebook, request.getNotebookGroupId());
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
      summary = "Update notebook readme content directly",
      description =
          "Saves the given markdown (with optional YAML frontmatter) as the notebook container's"
              + " readmeContent field. Blank content clears the field.")
  @PatchMapping("/{notebook}/readme-content")
  @Transactional
  public NotebookRealm updateNotebookReadmeContent(
      @PathVariable @Schema(type = "integer") Notebook notebook,
      @RequestBody NoteUpdateContentDTO dto)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    String content = dto.getContent();
    if (content != null && !content.isBlank()) {
      content = AuthoredNoteContent.prepareContentForSave(content);
    }
    notebook.setReadmeContent(content == null || content.isBlank() ? null : content);
    entityPersister.save(notebook);
    entityPersister.flush();
    User user = authorizationService.getCurrentUser();
    return notebookCatalogService.notebookRealmFor(notebook, user);
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

  @Operation(
      operationId = "resetNotebookGitHistory",
      summary = "Restart the notebook's Git history from its current content")
  @PostMapping("/{notebook}/reset-git-history")
  @Transactional
  public void resetNotebookGitHistory(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    notebookGitCutoverService.resetHistory(
        notebook, testabilitySettings.getCurrentUTCTimestamp().toInstant());
  }

  @Operation(
      operationId = "downloadNotebookGitBundle",
      summary = "Download the notebook's accepted Git bundle")
  @GetMapping(value = "/{notebook}/git-bundle", produces = "application/x-git-bundle")
  public ResponseEntity<byte[]> downloadNotebookGitBundle(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    byte[] bundleBytes = notebookGitBundleDownloadService.select(notebook.getId());
    String filename = "notebook-" + notebook.getId() + ".bundle";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.valueOf("application/x-git-bundle"))
        .body(bundleBytes);
  }

  @Operation(
      operationId = "publishNotebookGitProposal",
      summary = "Submit a proposal Git bundle to publish onto the notebook's accepted main")
  @PostMapping(value = "/{notebook}/git-bundle", consumes = "application/x-git-bundle")
  public String publishNotebookGitProposal(
      @PathVariable("notebook") @Schema(type = "integer") Integer notebookId,
      @RequestParam("expectedHead") String expectedHead,
      @RequestBody byte[] bundleBytes)
      throws UnexpectedNoAccessRightException {
    NotebookGitProposalImporter.ImportedProposal proposal =
        NotebookGitProposalImporter.importMainHead(bundleBytes);
    try {
      return notebookGitProposalPublisher.publish(notebookId, expectedHead, proposal);
    } finally {
      proposal.repository().close();
    }
  }
}
