package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NoteAiContextMarkdown;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextResult;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.focusContext.RetrievalConfig;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
class NoteExportController {

  private final AuthorizationService authorizationService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  NoteExportController(
      AuthorizationService authorizationService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer) {
    this.authorizationService = authorizationService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
  }

  /**
   * Focus-context markdown for this note (same render as {@link
   * com.odde.donut.services.focusContext.FocusContextMarkdownRenderer}). {@code tokenLimit} is the
   * combined focus-plus-related content budget, matching graph retrieval.
   */
  @GetMapping("/{note}/ai-context-markdown")
  public NoteAiContextMarkdown getAiContextMarkdown(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Parameter(
              description =
                  "Approximate token budget for focus note content plus related note content combined (bodies).")
          @RequestParam
          int tokenLimit)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();
    RetrievalConfig config = RetrievalConfig.forGraphApi(tokenLimit);
    FocusContextResult focusContextResult =
        focusContextRetrievalService.retrieve(note, user, config);
    String markdown = focusContextMarkdownRenderer.render(focusContextResult, config);
    return new NoteAiContextMarkdown(markdown);
  }
}
