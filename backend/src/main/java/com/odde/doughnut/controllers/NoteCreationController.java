package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.*;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.WikidataService;
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
      WikidataService wikidataService,
      NoteConstructionService noteConstructionService,
      AuthorizationService authorizationService) {
    this.wikidataService = wikidataService;
    this.noteConstructionService = noteConstructionService;
    this.authorizationService = authorizationService;
  }

  @PostMapping(value = "/{parentNote}/create")
  @Transactional
  public NoteRealm createNoteUnderParent(
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
}
