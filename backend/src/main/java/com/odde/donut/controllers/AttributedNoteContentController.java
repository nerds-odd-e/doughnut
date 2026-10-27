package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.AttributedNoteContent;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AttributedNoteContentService;
import com.odde.donut.services.AuthorizationService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
class AttributedNoteContentController {
  private final AuthorizationService authorizationService;
  private final AttributedNoteContentService attributedNoteContentService;

  AttributedNoteContentController(
      AuthorizationService authorizationService,
      AttributedNoteContentService attributedNoteContentService) {
    this.authorizationService = authorizationService;
    this.attributedNoteContentService = attributedNoteContentService;
  }

  @GetMapping("/{note}/attributed-content")
  public AttributedNoteContent getAttributedContent(
      @PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    return attributedNoteContentService.read(authorizationService.getCurrentUser(), note.getId());
  }
}
