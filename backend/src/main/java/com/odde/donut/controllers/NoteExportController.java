package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NoteAiContextMarkdown;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextResult;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.focusContext.RetrievalConfig;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
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
  private final McqRepository mcqRepository;
  private final RecallPromptRepository recallPromptRepository;

  NoteExportController(
      AuthorizationService authorizationService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      McqRepository mcqRepository,
      RecallPromptRepository recallPromptRepository) {
    this.authorizationService = authorizationService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
    this.mcqRepository = mcqRepository;
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
          boolean includeRecentQuestions)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();
    RetrievalConfig config = RetrievalConfig.forGraphApi(tokenLimit);
    FocusContextResult focusContextResult =
        focusContextRetrievalService.retrieve(note, user, config);
    String markdown = focusContextMarkdownRenderer.render(focusContextResult, config);
    if (includeRecentQuestions) {
      List<Mcq> questions =
          mcqRepository.findTop10ByNote_IdOrderByCreatedAtDescIdDesc(note.getId());
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
