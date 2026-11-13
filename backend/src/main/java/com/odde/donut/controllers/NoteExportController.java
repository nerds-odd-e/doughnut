package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NoteAiContextMarkdown;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteReferenceService;
import com.odde.donut.services.focusContext.FocusContextMarkdownAugmenter;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextResult;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.focusContext.ResolvedPropertyFocus;
import com.odde.donut.services.focusContext.RetrievalConfig;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notes")
class NoteExportController {

  private final AuthorizationService authorizationService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  private final McqRepository mcqRepository;
  private final NoteReferenceService noteReferenceService;
  private final RecallPromptRepository recallPromptRepository;

  NoteExportController(
      AuthorizationService authorizationService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      McqRepository mcqRepository,
      NoteReferenceService noteReferenceService,
      RecallPromptRepository recallPromptRepository) {
    this.authorizationService = authorizationService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
    this.mcqRepository = mcqRepository;
    this.noteReferenceService = noteReferenceService;
    this.recallPromptRepository = recallPromptRepository;
  }

  /**
   * Focus-context markdown for this note (same render as {@link
   * com.odde.donut.services.focusContext.FocusContextMarkdownRenderer}). {@code tokenLimit} is the
   * combined focus-plus-related content budget, matching graph retrieval. When {@code
   * includeRecentQuestions} is true, appends up to ten newest focus-note MCQs outside that budget,
   * with the current learner's latest own selection when present.
   */
  @GetMapping("/{note}/ai-context-markdown")
  public NoteAiContextMarkdown getAiContextMarkdown(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Parameter(
              description =
                  "Approximate token budget for focus note content plus related note content combined (bodies).")
          @RequestParam
          int tokenLimit,
      @Parameter(
              description =
                  "When true, append up to ten newest distinct focus-note multiple-choice questions with Donut answer keys and the current learner's latest own selection when present.")
          @RequestParam(defaultValue = "false")
          boolean includeRecentQuestions,
      @Parameter(description = "Exact authored property key to focus the report on.")
          @RequestParam(required = false)
          String propertyKey,
      @Parameter(description = "Exact raw list item value; omit to focus the whole property.")
          @RequestParam(required = false)
          String propertyValue,
      @Parameter(
              description =
                  "One-based occurrence among equal raw values; defaults to one for an item.")
          @RequestParam(required = false)
          Integer propertyOccurrence)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    ResolvedPropertyFocus selection = null;
    if (propertyKey != null) {
      selection =
          ResolvedPropertyFocus.resolve(note, propertyKey, propertyValue, propertyOccurrence)
              .orElseThrow(() ->
                  new ResponseStatusException(
                      HttpStatus.BAD_REQUEST,
                      "The highlighted property or item cannot be resolved. Close export, select an existing focus or clear the highlight, then reopen."));
    } else if (propertyValue != null || propertyOccurrence != null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A highlighted item requires a property key.");
    }
    User user = authorizationService.getCurrentUser();
    RetrievalConfig config = RetrievalConfig.forGraphApi(tokenLimit);
    FocusContextResult focusContextResult =
        focusContextRetrievalService.retrieve(note, user, config);
    String markdown = focusContextMarkdownRenderer.render(focusContextResult, config);
    if (selection != null) {
      markdown =
          FocusContextMarkdownAugmenter.embedPropertyFocus(
              markdown, FocusContextMarkdownAugmenter.buildPropertyFocusBlock(selection));
      markdown =
          FocusContextMarkdownAugmenter.ensureWikiLinks(
              markdown, noteReferenceService.wikiLinksForViewer(note, user));
    }
    if (includeRecentQuestions) {
      List<Mcq> questions =
          mcqRepository.findRecentForNote(
              note.getId(),
              selection == null ? null : selection.key(),
              selection == null ? null : selection.itemValue());
      markdown =
          markdown
              + RecentNoteQuestionsMarkdownAppendix.render(
                  questions, latestOwnChoiceIndexByMcqId(user, questions));
    }
    return new NoteAiContextMarkdown(markdown);
  }

  private Map<Integer, Integer> latestOwnChoiceIndexByMcqId(User user, List<Mcq> questions) {
    if (user == null || questions.isEmpty()) {
      return Map.of();
    }
    return recallPromptRepository.findLatestAnsweredChoiceIndexByMcqIdForUser(
        user.getId(), questions.stream().map(Mcq::getId).toList());
  }
}
