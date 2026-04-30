package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.*;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.NoteRealmService;
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
  private final NoteRealmService noteRealmService;

  @Autowired
  public NoteCreationController(
      WikidataService wikidataService,
      NoteConstructionService noteConstructionService,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService) {
    this.wikidataService = wikidataService;
    this.noteConstructionService = noteConstructionService;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
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

  @PostMapping(value = "/{referenceNote}/create-after")
  @Transactional
  public NoteRealm createNoteAfter(
      @PathVariable(name = "referenceNote") @Schema(type = "integer") Note referenceNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    authorizationService.assertAuthorization(referenceNote);
    if (referenceNote.getParent() == null) {
      throw new UnexpectedNoAccessRightException();
    }

    User user = authorizationService.getCurrentUser();
    Note note =
        noteConstructionService.createNoteAfter(
            referenceNote,
            noteCreation,
            wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId));

    return noteRealmService.build(note, user);
  }
}
