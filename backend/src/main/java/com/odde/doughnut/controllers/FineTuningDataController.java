package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.QuestionSuggestionParams;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.FineTuningService;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/fine-tuning")
class FineTuningDataController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;
  private final FineTuningService fineTuningService;
  private final OtherAiServices otherAiServices;

  public FineTuningDataController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      OpenAiApi openAiApi,
      OtherAiServices otherAiServices) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.fineTuningService = new FineTuningService(this.modelFactoryService, openAiApi);
    this.otherAiServices = otherAiServices;
  }

  @PatchMapping("/{suggestedQuestion}/update-suggested-question-for-fine-tuning")
  @Transactional
  public SuggestedQuestionForFineTuning updateSuggestedQuestionForFineTuning(
      @PathVariable("suggestedQuestion") @Schema(type = "integer")
          SuggestedQuestionForFineTuning suggestedQuestion,
      @RequestBody QuestionSuggestionParams suggestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return modelFactoryService
        .toSuggestedQuestionForFineTuningService(suggestedQuestion)
        .update(suggestion);
  }

  @PostMapping("/{suggestedQuestion}/duplicate")
  @Transactional
  public SuggestedQuestionForFineTuning duplicate(
      @PathVariable("suggestedQuestion") @Schema(type = "integer")
          SuggestedQuestionForFineTuning suggestedQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return modelFactoryService
        .toSuggestedQuestionForFineTuningService(suggestedQuestion)
        .duplicate();
  }

  @PostMapping("/{suggestedQuestion}/delete")
  @Transactional
  public SuggestedQuestionForFineTuning delete(
      @PathVariable("suggestedQuestion") @Schema(type = "integer")
          SuggestedQuestionForFineTuning suggestedQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return modelFactoryService.toSuggestedQuestionForFineTuningService(suggestedQuestion).delete();
  }

  @PostMapping("/upload-and-trigger-fine-tuning")
  @Transactional
  public void uploadAndTriggerFineTuning() throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertAdminAuthorization();
    List<OpenAIChatGPTFineTuningExample> examples1 =
        fineTuningService.getQuestionGenerationTrainingExamples();
    otherAiServices.uploadAndTriggerFineTuning(examples1, "Question");
    List<OpenAIChatGPTFineTuningExample> examples =
        fineTuningService.getQuestionEvaluationTrainingExamples();
    otherAiServices.uploadAndTriggerFineTuning(examples, "Evaluation");
  }

  @GetMapping("/all-suggested-questions-for-fine-tuning")
  public List<SuggestedQuestionForFineTuning> getAllSuggestedQuestions()
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return fineTuningService.getSuggestedQuestionForFineTunings();
  }
}
