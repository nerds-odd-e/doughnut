package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.*;
import com.odde.doughnut.services.*;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class NoteCreationController {
  private final WikidataService wikidataService;
  private final NoteConstructionService noteConstructionService;
  private final AuthorizationService authorizationService;

  @Autowired
  public NoteCreationController(
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings,
      NoteConstructionService noteConstructionService,
      AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
    this.wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
    this.noteConstructionService = noteConstructionService;
  }

  @PostMapping(value = "/{parentNote}/create")
  @Transactional
  public NoteCreationResult createNoteUnderParent(
      @PathVariable(name = "parentNote") @Schema(type = "integer") Note parentNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    authorizationService.assertAuthorization(parentNote);
    return noteConstructionService.createNoteWithWikidataService(
        parentNote,
        noteCreation,
        authorizationService.getCurrentUser(),
        wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId));
  }

  @PostMapping(value = "/{referenceNote}/create-after")
  @Transactional
  public NoteCreationResult createNoteAfter(
      @PathVariable(name = "referenceNote") @Schema(type = "integer") Note referenceNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    authorizationService.assertAuthorization(referenceNote);
    if (referenceNote.getParent() == null) {
      throw new UnexpectedNoAccessRightException();
    }

    Note note =
        noteConstructionService.createNoteAfter(
            referenceNote,
            noteCreation,
            authorizationService.getCurrentUser(),
            wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId));

    return new NoteCreationResult(
        note.toNoteRealm(authorizationService.getCurrentUser()),
        note.getParent().toNoteRealm(authorizationService.getCurrentUser()));
  }
}
