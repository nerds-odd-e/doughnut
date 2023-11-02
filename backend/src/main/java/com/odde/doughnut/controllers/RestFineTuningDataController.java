package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.controllers.json.QuestionSuggestionParams;
import com.odde.doughnut.controllers.json.UploadFineTuningExamplesResponse;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.FineTuningService;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/fine-tuning")
class RestFineTuningDataController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;
  private final FineTuningService fineTuningService;

  public RestFineTuningDataController(
      ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.fineTuningService = new FineTuningService(this.modelFactoryService);
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

  @PostMapping("/{suggestedQuestion}/delete")
  public SuggestedQuestionForFineTuning delete(
      @PathVariable("suggestedQuestion") SuggestedQuestionForFineTuning suggestedQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return modelFactoryService.toSuggestedQuestionForFineTuningService(suggestedQuestion).delete();
  }

  @GetMapping("/positive-feedback-generation-examples")
  public List<OpenAIChatGPTFineTuningExample>
      getAllPositiveFeedbackQuestionGenerationFineTuningExamples()
          throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return fineTuningService.getQuestionGenerationTrainingExamples();
  }

  @PostMapping("/upload-fine-tuning-examples")
  public UploadFineTuningExamplesResponse uploadFineTuningExamples() {
    return fineTuningService.getUploadFineTuningExamplesResponse();
  }

  @GetMapping("/feedback-evaluation-examples")
  public List<OpenAIChatGPTFineTuningExample> getAllEvaluationExamples()
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return fineTuningService.getQuestionEvaluationTrainingExamples();
  }

  @GetMapping("/all-suggested-questions-for-fine-tuning")
  public List<SuggestedQuestionForFineTuning> getAllSuggestedQuestions()
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return fineTuningService.getSuggestedQuestionForFineTunings();
  }
}
