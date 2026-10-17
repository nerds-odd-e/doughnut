package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.AttributedNoteContent;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.focusContext.FocusContextConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
class AttributedNoteContentController {
  private final AuthorizationService authorizationService;
  private final NotebookGitBindingRepository bindingRepository;

  AttributedNoteContentController(
      AuthorizationService authorizationService, NotebookGitBindingRepository bindingRepository) {
    this.authorizationService = authorizationService;
    this.bindingRepository = bindingRepository;
  }

  @GetMapping("/{note}/attributed-content")
  @Transactional(readOnly = true)
  public AttributedNoteContent getAttributedContent(
      @PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    return new AttributedNoteContent(
        note.getId(),
        note.getTitle(),
        note.getNotebook().getName(),
        FocusContextConstants.folderPathOf(note),
        note.getContent(),
        note.getUpdatedAt(),
        bindingRepository.findAcceptedRevision(note.getNotebook().getId()));
  }
}
