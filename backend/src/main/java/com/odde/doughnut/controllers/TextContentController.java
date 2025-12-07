package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateDetailsDTO;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.function.Consumer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text_content")
class TextContentController {
  private final EntityPersister entityPersister;

  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;

  public TextContentController(
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
  }

  @PatchMapping(path = "/{note}/title")
  @Transactional
  public NoteRealm updateNoteTitle(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateTitleDTO titleDTO)
      throws UnexpectedNoAccessRightException {
    return updateNote(note, n -> n.setTitleConstructor(titleDTO.getNewTitle()));
  }

  @PatchMapping(path = "/{note}/details")
  @Transactional
  public NoteRealm updateNoteDetails(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateDetailsDTO detailsDTO)
      throws UnexpectedNoAccessRightException {
    return updateNote(note, n -> n.setDetails(detailsDTO.getDetails()));
  }

  private NoteRealm updateNote(Note note, Consumer<Note> updateFunction)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    note.setUpdatedAt(currentUTCTimestamp);
    updateFunction.accept(note);
    entityPersister.save(note);
    return note.toNoteRealm(authorizationService.getCurrentUser());
  }
}
