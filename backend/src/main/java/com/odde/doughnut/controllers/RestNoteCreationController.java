package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.*;
import com.odde.doughnut.services.*;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class RestNoteCreationController {
  private final UserModel currentUser;
  private final WikidataService wikidataService;
  private final NoteConstructionService noteConstructionService;

  public RestNoteCreationController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings) {
    this.currentUser = currentUser;
    this.wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
    this.noteConstructionService =
        new NoteConstructionService(
            currentUser.getEntity(),
            testabilitySettings.getCurrentUTCTimestamp(),
            modelFactoryService);
  }

  @PostMapping(value = "/{parentNote}/create")
  @Transactional
  public NoteCreationResult createNote(
      @PathVariable(name = "parentNote") @Schema(type = "integer") Note parentNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    currentUser.assertAuthorization(parentNote);
    return noteConstructionService.createNoteWithWikidataService(
        parentNote,
        noteCreation,
        currentUser.getEntity(),
        wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId));
  }

  @PostMapping(value = "/{referenceNote}/create-after")
  @Transactional
  public NoteCreationResult createNoteAfter(
      @PathVariable(name = "referenceNote") @Schema(type = "integer") Note referenceNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    currentUser.assertAuthorization(referenceNote);
    if (referenceNote.getParent() == null) {
      throw new UnexpectedNoAccessRightException();
    }

    Note note =
        noteConstructionService.createNoteAfter(
            referenceNote,
            noteCreation,
            currentUser.getEntity(),
            wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId));

    return new NoteCreationResult(
        new NoteViewer(currentUser.getEntity(), note).toJsonObject(),
        new NoteViewer(currentUser.getEntity(), note.getParent()).toJsonObject());
  }
}
