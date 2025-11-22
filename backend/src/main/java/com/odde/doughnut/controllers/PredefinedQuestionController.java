package com.odde.doughnut.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.PredefinedQuestionService;
import com.odde.doughnut.services.QuestionGenerationRequestBuilder;
import com.odde.doughnut.services.SuggestedQuestionForFineTuningService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.TestabilitySettings;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predefined-questions")
class PredefinedQuestionController {
  private final PredefinedQuestionService predefinedQuestionService;
  private final SuggestedQuestionForFineTuningService suggestedQuestionForFineTuningService;

  private final TestabilitySettings testabilitySettings;

  private final AiQuestionGenerator aiQuestionGenerator;
  private final ObjectMapper objectMapper;
  private final AuthorizationService authorizationService;
  private final GlobalSettingsService globalSettingsService;

  @Autowired
  public PredefinedQuestionController(
      PredefinedQuestionService predefinedQuestionService,
      SuggestedQuestionForFineTuningService suggestedQuestionForFineTuningService,
      TestabilitySettings testabilitySettings,
      ObjectMapper objectMapper,
      AuthorizationService authorizationService,
      GlobalSettingsService globalSettingsService,
      AiQuestionGenerator aiQuestionGenerator) {
    this.predefinedQuestionService = predefinedQuestionService;
    this.suggestedQuestionForFineTuningService = suggestedQuestionForFineTuningService;
    this.testabilitySettings = testabilitySettings;
    this.objectMapper = objectMapper;
    this.authorizationService = authorizationService;
    this.globalSettingsService = globalSettingsService;
    this.aiQuestionGenerator = aiQuestionGenerator;
  }

  @PostMapping("/generate-question-without-save")
  public PredefinedQuestion generateQuestionWithoutSave(
      @RequestParam(value = "note") @Schema(type = "integer") Note note) {
    authorizationService.assertLoggedIn();
    MCQWithAnswer MCQWithAnswer = aiQuestionGenerator.getAiGeneratedQuestion(note, null);
    if (MCQWithAnswer == null) {
      return null;
    }
    return PredefinedQuestion.fromMCQWithAnswer(MCQWithAnswer, note);
  }

  @PostMapping("/{predefinedQuestion}/suggest-fine-tuning")
  @Transactional
  public SuggestedQuestionForFineTuning suggestQuestionForFineTuning(
      @PathVariable("predefinedQuestion") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion,
      @Valid @RequestBody QuestionSuggestionCreationParams suggestion) {
    SuggestedQuestionForFineTuning sqft = new SuggestedQuestionForFineTuning();
    return suggestedQuestionForFineTuningService.suggestQuestionForFineTuning(
        sqft,
        predefinedQuestion,
        suggestion,
        authorizationService.getCurrentUser(),
        testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping("/{note}/note-questions")
  public List<PredefinedQuestion> getAllQuestionByNote(
      @PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    return note.getPredefinedQuestions().stream().toList();
  }

  @PostMapping("/{note}/note-questions")
  @Transactional
  public PredefinedQuestion addQuestionManually(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody PredefinedQuestion predefinedQuestion)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    return predefinedQuestionService.addQuestion(note, predefinedQuestion);
  }

  @PostMapping("/{note}/refine-question")
  @Transactional
  public PredefinedQuestion refineQuestion(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @RequestBody PredefinedQuestion predefinedQuestion)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    return predefinedQuestionService.refineAIQuestion(note, predefinedQuestion);
  }

  @PostMapping("/{predefinedQuestion}/toggle-approval")
  @Transactional
  public PredefinedQuestion toggleApproval(
      @PathVariable("predefinedQuestion") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(predefinedQuestion.getNote());
    return predefinedQuestionService.toggleApproval(predefinedQuestion);
  }

  @GetMapping(value = "/{note}/export-question-generation", produces = "application/json")
  public ChatCompletionCreateParams exportQuestionGeneration(
      @PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    QuestionGenerationRequestBuilder requestBuilder =
        new QuestionGenerationRequestBuilder(globalSettingsService, objectMapper);
    return requestBuilder.buildQuestionGenerationRequest(note, null);
  }
}
