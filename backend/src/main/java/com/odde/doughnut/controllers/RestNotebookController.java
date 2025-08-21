package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.controllers.dto.RedirectToNoteResponse;
import com.odde.doughnut.controllers.dto.UpdateAiAssistantRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.JsonViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.NotebookReindexingService;
import com.odde.doughnut.services.ObsidianFormatService;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
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
class RestNotebookController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final ObsidianFormatService obsidianFormatService;
  private final NotebookReindexingService notebookReindexingService;

  public RestNotebookController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings,
      NotebookReindexingService notebookReindexingService) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.notebookReindexingService = notebookReindexingService;
    this.obsidianFormatService =
        new ObsidianFormatService(currentUser.getEntity(), modelFactoryService);
  }

  @GetMapping("")
  public NotebooksViewedByUser myNotebooks() {
    currentUser.assertLoggedIn();

    User user = currentUser.getEntity();
    NotebooksViewedByUser notebooksViewedByUser =
        new JsonViewer().jsonNotebooksViewedByUser(user.getOwnership().getNotebooks());
    notebooksViewedByUser.subscriptions = user.getSubscriptions();
    return notebooksViewedByUser;
  }

  @PostMapping({"/create"})
  @Transactional
  public RedirectToNoteResponse createNotebook(@Valid @RequestBody NoteCreationDTO noteCreation) {
    currentUser.assertLoggedIn();
    User userEntity = currentUser.getEntity();
    Note note =
        userEntity
            .getOwnership()
            .createAndPersistNotebook(
                userEntity, testabilitySettings.getCurrentUTCTimestamp(),
                modelFactoryService, noteCreation.getNewTitle());
    return new RedirectToNoteResponse(note.getId());
  }

  @PostMapping(value = "/{notebook}")
  @Transactional
  public Notebook update(
      @PathVariable @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody NotebookSettings notebookSettings)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    notebook.getNotebookSettings().update(notebookSettings);
    modelFactoryService.save(notebook);
    return notebook;
  }

  @GetMapping(value = "/{notebook}")
  public Notebook get(@PathVariable @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    return notebook;
  }

  @PostMapping(value = "/{notebook}/share")
  @Transactional
  public Notebook shareNotebook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    BazaarModel bazaar = modelFactoryService.toBazaarModel();
    bazaar.shareNotebook(notebook);
    return notebook;
  }

  @PatchMapping(value = "/{notebook}/move-to-circle/{circle}")
  @Transactional
  public Notebook moveToCircle(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("circle") @Schema(type = "integer") Circle circle)
      throws UnexpectedNoAccessRightException {
    if (notebook.getCreatorEntity().getId() != currentUser.getEntity().getId()) {
      throw new UnexpectedNoAccessRightException();
    }
    notebook.setOwnership(circle.getOwnership());
    modelFactoryService.save(notebook);
    return notebook;
  }

  @GetMapping("/{notebook}/dump")
  public List<BareNote> downloadNotebookDump(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    return notebook.getNoteBriefs();
  }

  @GetMapping("{notebook}/notes")
  public List<Note> getNotes(@PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    return notebook.getNotes();
  }

  @PatchMapping("/{notebook}/ai-assistant")
  @Transactional
  public NotebookAiAssistant updateAiAssistant(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody UpdateAiAssistantRequest request)
      throws UnexpectedNoAccessRightException {

    currentUser.assertAuthorization(notebook);

    NotebookAiAssistant assistant =
        modelFactoryService.notebookAiAssistantRepository.findByNotebookId(notebook.getId());
    if (assistant == null) {
      assistant = new NotebookAiAssistant();
      assistant.setNotebook(notebook);
      assistant.setCreatedAt(testabilitySettings.getCurrentUTCTimestamp());
    }

    assistant.setAdditionalInstructionsToAi(request.getAdditionalInstructions());
    assistant.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());

    return modelFactoryService.notebookAiAssistantRepository.save(assistant);
  }

  @GetMapping("/{notebook}/ai-assistant")
  public NotebookAiAssistant getAiAssistant(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {

    currentUser.assertAuthorization(notebook);
    return modelFactoryService.notebookAiAssistantRepository.findByNotebookId(notebook.getId());
  }

  @GetMapping("/{notebook}/obsidian")
  public ResponseEntity<byte[]> downloadNotebookForObsidian(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertAuthorization(notebook);

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
  @PostMapping(value = "/{notebookId}/obsidian", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Transactional
  public void importObsidian(
      @Parameter(description = "Obsidian zip file to import") @RequestParam("file")
          MultipartFile file,
      @Parameter(description = "Notebook ID") @PathVariable("notebookId") @Schema(type = "integer")
          Notebook notebook)
      throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(notebook);
    obsidianFormatService.importFromObsidian(file, notebook);
  }

  @PostMapping("/{notebook}/reindex")
  @Transactional
  public void reindexNotebook(@PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    notebookReindexingService.reindexNotebook(notebook);
  }

  @PostMapping("/{notebook}/update-index")
  @Transactional
  public void updateNotebookIndex(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    notebookReindexingService.updateNotebookIndex(notebook);
  }
}
