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
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class RestNoteCreationController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;
  private final WikidataService wikidataService;
  private final TestabilitySettings testabilitySettings;

  public RestNoteCreationController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
  }

  @PostMapping(value = "/{parentNote}/create")
  public NoteCreationRresult createNote(
      @PathVariable(name = "parentNote") @Schema(type = "integer") Note parentNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    currentUser.assertAuthorization(parentNote);
    return getNoteConstructionService(currentUser.getEntity())
        .createNoteInternal(parentNote, noteCreation, currentUser.getEntity(), wikidataService);
  }

  @PostMapping(value = "/{referenceNote}/create-after")
  public NoteCreationRresult createNoteAfter(
      @PathVariable(name = "referenceNote") @Schema(type = "integer") Note referenceNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    currentUser.assertAuthorization(referenceNote);
    Note parentNote = referenceNote.getParent();
    if (parentNote == null) {
      throw new UnexpectedNoAccessRightException();
    }

    Note note =
        getNoteConstructionService(currentUser.getEntity())
            .createNoteAfter(
                referenceNote, noteCreation, parentNote, currentUser.getEntity(), wikidataService);

    return new NoteCreationRresult(
        new NoteViewer(currentUser.getEntity(), note).toJsonObject(),
        new NoteViewer(currentUser.getEntity(), parentNote).toJsonObject());
  }

  private NoteConstructionService getNoteConstructionService(User user) {
    return new NoteConstructionService(
        user, testabilitySettings.getCurrentUTCTimestamp(), modelFactoryService);
  }
}
