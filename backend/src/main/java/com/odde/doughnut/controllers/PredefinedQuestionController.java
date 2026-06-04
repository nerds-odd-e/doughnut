package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NoteRealmService;
import com.odde.doughnut.services.PredefinedQuestionService;
import com.odde.doughnut.services.QuestionGenerationRequestBuilder;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.openai.models.responses.StructuredResponseCreateParams;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predefined-questions")
class PredefinedQuestionController {
  private final PredefinedQuestionService predefinedQuestionService;

  private final AiQuestionGenerator aiQuestionGenerator;
  private final ObjectMapper objectMapper;
  private final AuthorizationService authorizationService;
  private final GlobalSettingsService globalSettingsService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  private final NoteRealmService noteRealmService;
  private final NoteRepository noteRepository;
  private final WikiTitleCacheService wikiTitleCacheService;

  @Autowired
  public PredefinedQuestionController(
      PredefinedQuestionService predefinedQuestionService,
      ObjectMapper objectMapper,
      AuthorizationService authorizationService,
      GlobalSettingsService globalSettingsService,
      AiQuestionGenerator aiQuestionGenerator,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      NoteRealmService noteRealmService,
      NoteRepository noteRepository,
      WikiTitleCacheService wikiTitleCacheService) {
    this.predefinedQuestionService = predefinedQuestionService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
    this.noteRealmService = noteRealmService;
    this.noteRepository = noteRepository;
    this.wikiTitleCacheService = wikiTitleCacheService;
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

  @GetMapping(value = "/{note}/export-question-generation", produces = "application/json")
  public Map<String, Object> exportQuestionGeneration(
      @PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    QuestionGenerationRequestBuilder requestBuilder =
        new QuestionGenerationRequestBuilder(
            globalSettingsService,
            focusContextRetrievalService,
            focusContextMarkdownRenderer,
            noteRealmService,
            noteRepository,
            wikiTitleCacheService,
            authorizationService);
    StructuredResponseCreateParams<MCQWithAnswer> params =
        requestBuilder.buildQuestionGenerationResponseRequest(note, null, null, null);
    return serializeResponseCreateParams(params);
  }

  private Map<String, Object> serializeResponseCreateParams(
      StructuredResponseCreateParams<?> params) {
    try {
      Method bodyMethod = params.rawParams().getClass().getMethod("_body");
      Object body = bodyMethod.invoke(params.rawParams());
      // Serialize the Body using ObjectMapper, which handles JsonField properly
      String jsonString = objectMapper.writeValueAsString(body);
      // Convert back to Map to ensure all non-empty fields are included
      Map<String, Object> result =
          objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
      // Remove internal "valid" fields from the SDK
      removeValidFields(result);
      return result;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize ResponseCreateParams", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void removeValidFields(Object obj) {
    if (obj == null) {
      return;
    }
    if (obj instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) obj;
      map.remove("valid");
      for (Object value : map.values()) {
        removeValidFields(value);
      }
    } else if (obj instanceof List) {
      List<?> list = (List<?>) obj;
      for (Object item : list) {
        removeValidFields(item);
      }
    }
  }
}
