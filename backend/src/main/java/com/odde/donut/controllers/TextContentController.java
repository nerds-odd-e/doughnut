package com.odde.donut.controllers;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteRealmService;
import com.odde.donut.services.notebookGit.WebNoteEditService;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text_content")
class TextContentController {
  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;
  private final WebNoteEditService webNoteEditService;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public TextContentController(
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService,
      WebNoteEditService webNoteEditService,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
    this.webNoteEditService = webNoteEditService;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
  }

  @PatchMapping(path = "/{note}/title")
  public NoteRealm updateNoteTitle(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateTitleDTO titleDTO)
      throws UnexpectedNoAccessRightException {
    Note savedNote =
        webNoteEditService.saveTitle(
            note.getId(),
            note.getNotebook().getId(),
            titleDTO,
            testabilitySettings.getCurrentUTCTimestamp());
    return noteRealmService.build(savedNote, authorizationService.getCurrentUser());
  }

  @PatchMapping(path = "/{note}/content")
  public NoteRealm updateNoteContent(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateContentDTO contentDTO)
      throws UnexpectedNoAccessRightException {
    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave(contentDTO.getContent(), canonicalDonutOrigin);
    Note savedNote =
        webNoteEditService.saveContent(
            note.getId(),
            note.getNotebook().getId(),
            document,
            testabilitySettings.getCurrentUTCTimestamp());
    return noteRealmService.build(savedNote, authorizationService.getCurrentUser());
  }
}
