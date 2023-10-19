package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.controllers.json.QuestionSuggestionParams;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/fine-tuning")
class RestFineTuningDataController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  public RestFineTuningDataController(
      ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @PatchMapping("/{suggestedQuestion}/update-suggested-question-for-fine-tuning")
  @Transactional
  public SuggestedQuestionForFineTuning updateSuggestedQuestionForFineTuning(
      @PathVariable("suggestedQuestion") SuggestedQuestionForFineTuning suggestedQuestion,
      @RequestBody QuestionSuggestionParams suggestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return modelFactoryService
        .toSuggestedQuestionForFineTuningService(suggestedQuestion)
        .update(suggestion);
  }

  @PostMapping("/{suggestedQuestion}/duplicate")
  public SuggestedQuestionForFineTuning duplicate(
      @PathVariable("suggestedQuestion") SuggestedQuestionForFineTuning suggestedQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return modelFactoryService
        .toSuggestedQuestionForFineTuningService(suggestedQuestion)
        .duplicate();
  }

  @GetMapping("/positive-feedback-generation-examples")
  public List<OpenAIChatGPTFineTuningExample>
      getAllPositiveFeedbackQuestionGenerationFineTuningExamples()
          throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return getSuggestedQuestionForFineTunings().stream()
        .filter(question -> question.isPositiveFeedback())
        .map(SuggestedQuestionForFineTuning::toFineTuningExample)
        .toList();
  }

  @GetMapping("/feedback-evaluation-examples")
  public List<OpenAIChatGPTFineTuningExample> getAllEvaluationExamples()
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return getSuggestedQuestionForFineTunings().stream()
        .map(SuggestedQuestionForFineTuning::toEvaluationData)
        .toList();
  }

  @GetMapping("/all-suggested-questions-for-fine-tuning")
  public List<SuggestedQuestionForFineTuning> getAllSuggestedQuestions()
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return getSuggestedQuestionForFineTunings();
  }

  private List<SuggestedQuestionForFineTuning> getSuggestedQuestionForFineTunings() {
    List<SuggestedQuestionForFineTuning> suggestedQuestionForFineTunings = new ArrayList<>();
    modelFactoryService
        .questionSuggestionForFineTuningRepository
        .findAll()
        .forEach(suggestedQuestionForFineTunings::add);
    return suggestedQuestionForFineTunings;
  }
}
