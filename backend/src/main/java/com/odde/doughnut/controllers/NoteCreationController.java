package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.*;
import com.odde.doughnut.services.*;
import com.odde.doughnut.services.AuthorizationService;
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
class NoteCreationController {
  private final CurrentUser currentUser;
  private final WikidataService wikidataService;
  private final NoteConstructionService noteConstructionService;
  private final AuthorizationService authorizationService;

  public NoteCreationController(
      ModelFactoryService modelFactoryService,
      CurrentUser currentUser,
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings,
      NoteService noteService,
      AuthorizationService authorizationService) {
    this.currentUser = currentUser;
    this.authorizationService = authorizationService;
    this.wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
    this.noteConstructionService =
        new NoteConstructionService(
            currentUser.getUser(),
            testabilitySettings.getCurrentUTCTimestamp(),
            modelFactoryService,
            noteService);
  }

  @PostMapping(value = "/{parentNote}/create")
  @Transactional
  public NoteCreationResult createNoteUnderParent(
      @PathVariable(name = "parentNote") @Schema(type = "integer") Note parentNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    authorizationService.assertAuthorization(currentUser.getUser(), parentNote);
    return noteConstructionService.createNoteWithWikidataService(
        parentNote,
        noteCreation,
        currentUser.getUser(),
        wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId));
  }

  @PostMapping(value = "/{referenceNote}/create-after")
  @Transactional
  public NoteCreationResult createNoteAfter(
      @PathVariable(name = "referenceNote") @Schema(type = "integer") Note referenceNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    authorizationService.assertAuthorization(currentUser.getUser(), referenceNote);
    if (referenceNote.getParent() == null) {
      throw new UnexpectedNoAccessRightException();
    }

    Note note =
        noteConstructionService.createNoteAfter(
            referenceNote,
            noteCreation,
            currentUser.getUser(),
            wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId));

    return new NoteCreationResult(
        note.toNoteRealm(currentUser.getUser()),
        note.getParent().toNoteRealm(currentUser.getUser()));
  }
}
