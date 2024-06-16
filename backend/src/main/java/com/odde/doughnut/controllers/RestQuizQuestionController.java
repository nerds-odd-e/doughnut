package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.controllers.dto.QuizQuestionContestResult;
import com.odde.doughnut.controllers.dto.QuizQuestionInNotebook;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.AnswerModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.QuizQuestionService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz-questions")
class RestQuizQuestionController {
  private final ModelFactoryService modelFactoryService;
  private final QuizQuestionService quizQuestionService;

  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AiQuestionGenerator aiQuestionGenerator;

  public RestQuizQuestionController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.aiQuestionGenerator =
        new AiQuestionGenerator(openAiApi, new GlobalSettingsService(modelFactoryService));
    this.quizQuestionService = new QuizQuestionService(openAiApi, modelFactoryService);
  }

  @PostMapping("/generate-question")
  @Transactional
  public QuizQuestionInNotebook generateQuestion(
      @RequestParam(value = "note") @Schema(type = "integer") Note note) {
    currentUser.assertLoggedIn();
    QuizQuestionAndAnswer quizQuestionAndAnswer = quizQuestionService.generateQuestionForNote(note);
    if (quizQuestionAndAnswer == null) {
      return null;
    }
    return quizQuestionAndAnswer.toQuizQuestionInNotebook();
  }

  @PostMapping("/{quizQuestion}/regenerate")
  @Transactional
  public QuizQuestion regenerate(
      @PathVariable("quizQuestion") @Schema(type = "integer")
          QuizQuestionAndAnswer quizQuestionAndAnswer) {
    currentUser.assertLoggedIn();
    return quizQuestionService
        .generateQuestionForNote(quizQuestionAndAnswer.getNote())
        .getQuizQuestion();
  }

  @PostMapping("/generate-question-without-save")
  public MCQWithAnswer generateAIQuestionWithoutSave(
      @RequestParam(value = "note") @Schema(type = "integer") Note note) {
    currentUser.assertLoggedIn();
    return quizQuestionService.generateMcqWithAnswer(note);
  }

  @PostMapping("/{quizQuestion}/contest")
  @Transactional
  public QuizQuestionContestResult contest(
      @PathVariable("quizQuestion") @Schema(type = "integer")
          QuizQuestionAndAnswer quizQuestionAndAnswer) {
    currentUser.assertLoggedIn();
    return aiQuestionGenerator.getQuizQuestionContestResult(quizQuestionAndAnswer);
  }

  @PostMapping("/{quizQuestion}/answer")
  @Transactional
  public AnsweredQuestion answerQuiz(
      @PathVariable("quizQuestion") @Schema(type = "integer")
          QuizQuestionAndAnswer quizQuestionAndAnswer,
      @Valid @RequestBody AnswerDTO answerDTO) {
    currentUser.assertLoggedIn();
    Answer answer = new Answer();
    answer.setQuestion(quizQuestionAndAnswer);
    answer.setFromDTO(answerDTO);
    AnswerModel answerModel = modelFactoryService.toAnswerModel(answer);
    answerModel.makeAnswerToQuestion(
        testabilitySettings.getCurrentUTCTimestamp(), currentUser.getEntity());
    return answerModel.getAnswerViewedByUser(currentUser.getEntity());
  }

  @PostMapping("/{quizQuestion}/suggest-fine-tuning")
  @Transactional
  public SuggestedQuestionForFineTuning suggestQuestionForFineTuning(
      @PathVariable("quizQuestion") @Schema(type = "integer")
          QuizQuestionAndAnswer quizQuestionAndAnswer,
      @Valid @RequestBody QuestionSuggestionCreationParams suggestion) {
    SuggestedQuestionForFineTuning sqft = new SuggestedQuestionForFineTuning();
    var suggestedQuestionForFineTuningService =
        modelFactoryService.toSuggestedQuestionForFineTuningService(sqft);
    return suggestedQuestionForFineTuningService.suggestQuestionForFineTuning(
        quizQuestionAndAnswer,
        suggestion,
        currentUser.getEntity(),
        testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping("/{note}/note-questions")
  public List<MCQWithAnswer> getAllQuestionByNote(
      @PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    return note.getQuizQuestionAndAnswers().stream()
        .map(QuizQuestionAndAnswer::getMcqWithAnswer)
        .toList();
  }

  @PostMapping("/{note}/note-questions")
  @Transactional
  public MCQWithAnswer addQuestionManually(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody MCQWithAnswer manualQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    return quizQuestionService.addQuestion(note, manualQuestion).getMcqWithAnswer();
  }

  @PostMapping("/{note}/refine-question")
  @Transactional
  public MCQWithAnswer refineQuestion(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @RequestBody MCQWithAnswer manualQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    return quizQuestionService.refineQuestion(note, manualQuestion);
  }

  @PostMapping("/{quizQuestion}/toggle-approval")
  @Transactional
  public QuizQuestionAndAnswer toggleApproval(
      @PathVariable("quizQuestion") @Schema(type = "integer")
          QuizQuestionAndAnswer quizQuestionAndAnswer)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(quizQuestionAndAnswer.getNote());
    return quizQuestionService.toggleApproval(quizQuestionAndAnswer);
  }
}
