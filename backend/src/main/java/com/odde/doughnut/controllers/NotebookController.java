package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.controllers.dto.RedirectToNoteResponse;
import com.odde.doughnut.controllers.dto.UpdateAiAssistantRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.BazaarService;
import com.odde.doughnut.services.NotebookIndexingService;
import com.odde.doughnut.services.NotebookService;
import com.odde.doughnut.services.ObsidianFormatService;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.multipart.MultipartFile;

@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookController {
  private final EntityPersister entityPersister;
  private final NotebookService notebookService;

  private final TestabilitySettings testabilitySettings;

  private final ObsidianFormatService obsidianFormatService;
  private final NotebookIndexingService notebookIndexingService;
  private final BazaarService bazaarService;
  private final AuthorizationService authorizationService;

  public NotebookController(
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NotebookIndexingService notebookIndexingService,
      BazaarService bazaarService,
      AuthorizationService authorizationService,
      NotebookService notebookService,
      ObsidianFormatService obsidianFormatService) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.notebookIndexingService = notebookIndexingService;
    this.bazaarService = bazaarService;
    this.authorizationService = authorizationService;
    this.notebookService = notebookService;
    this.obsidianFormatService = obsidianFormatService;
  }

  @GetMapping("")
  public NotebooksViewedByUser myNotebooks() {
    authorizationService.assertLoggedIn();

    User user = authorizationService.getCurrentUser();
    NotebooksViewedByUser notebooksViewedByUser =
        user.getOwnership().jsonNotebooksViewedByUser(user.getOwnership().getNotebooks());
    notebooksViewedByUser.subscriptions = user.getSubscriptions();
    return notebooksViewedByUser;
  }

  @PostMapping({"/create"})
  @Transactional
  public RedirectToNoteResponse createNotebook(@Valid @RequestBody NoteCreationDTO noteCreation) {
    authorizationService.assertLoggedIn();
    User userEntity = authorizationService.getCurrentUser();
    Note note =
        notebookService.createNotebookForOwnership(
            userEntity.getOwnership(),
            userEntity,
            testabilitySettings.getCurrentUTCTimestamp(),
            noteCreation.getNewTitle());
    return new RedirectToNoteResponse(note.getId());
  }

  @PostMapping(value = "/{notebook}")
  @Transactional
  public Notebook updateNotebook(
      @PathVariable @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody NotebookSettings notebookSettings)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    notebook.getNotebookSettings().update(notebookSettings);
    entityPersister.save(notebook);
    return notebook;
  }

  @GetMapping(value = "/{notebook}")
  public Notebook get(@PathVariable @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    return notebook;
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

  @GetMapping("/{notebook}/dump")
  public List<BareNote> downloadNotebookDump(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return notebook.getNoteBriefs();
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

  @GetMapping("/{notebook}/obsidian")
  public ResponseEntity<byte[]> downloadNotebookForObsidian(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException, IOException {
    authorizationService.assertAuthorization(notebook);

    byte[] zipBytes = obsidianFormatService.exportToObsidian(notebook.getHeadNote());

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + sanitizeFileName(notebook.getTitle()) + "-obsidian.zip\"")
        .header(HttpHeaders.CONTENT_TYPE, "application/zip")
        .body(zipBytes);
  }

  private String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\/:*?\"<>|]", "_");
  }

  @Operation(summary = "Import Obsidian file")
  @PostMapping(value = "/{notebook}/obsidian", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Transactional
  public void importObsidian(
      @Parameter(description = "Obsidian zip file to import") @RequestParam("file")
          MultipartFile file,
      @Parameter(description = "Notebook ID") @PathVariable("notebook") @Schema(type = "integer")
          Notebook notebook)
      throws UnexpectedNoAccessRightException, IOException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(notebook);
    obsidianFormatService.importFromObsidian(file, notebook);
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
