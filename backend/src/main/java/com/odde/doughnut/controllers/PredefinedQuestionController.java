package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.PredefinedQuestionService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.openAiApis.StructuredResponseCreateParamsSerializer;
import com.openai.models.responses.StructuredResponseCreateParams;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
  private final AuthorizationService authorizationService;
  private final NoteQuestionGenerationService noteQuestionGenerationService;
  private final StructuredResponseCreateParamsSerializer paramsSerializer;

  @Autowired
  public PredefinedQuestionController(
      PredefinedQuestionService predefinedQuestionService,
      AuthorizationService authorizationService,
      AiQuestionGenerator aiQuestionGenerator,
      NoteQuestionGenerationService noteQuestionGenerationService,
      StructuredResponseCreateParamsSerializer paramsSerializer) {
    this.predefinedQuestionService = predefinedQuestionService;
    this.authorizationService = authorizationService;
    this.aiQuestionGenerator = aiQuestionGenerator;
    this.noteQuestionGenerationService = noteQuestionGenerationService;
    this.paramsSerializer = paramsSerializer;
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

  @PostMapping("/{predefinedQuestion}/delete")
  @Transactional
  public PredefinedQuestion deleteQuestion(
      @PathVariable("predefinedQuestion") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(predefinedQuestion.getNote());
    return predefinedQuestionService.deleteQuestion(predefinedQuestion);
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
    StructuredResponseCreateParams<MCQWithAnswer> params =
        noteQuestionGenerationService.buildQuestionGenerationRequest(note, null);
    return paramsSerializer.toBodyMap(params);
  }
}
