package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.QuestionSuggestionParams;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.FineTuningService;
import com.odde.doughnut.services.SuggestedQuestionForFineTuningService;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.services.ai.OtherAiServices;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RequiredArgsConstructor
@RestController
@SessionScope
@RequestMapping("/api/fine-tuning")
class FineTuningDataController {
  private final FineTuningService fineTuningService;
  private final OtherAiServices otherAiServices;
  private final SuggestedQuestionForFineTuningService suggestedQuestionForFineTuningService;
  private final AuthorizationService authorizationService;

  @PatchMapping("/{suggestedQuestion}/update-suggested-question-for-fine-tuning")
  @Transactional
  public SuggestedQuestionForFineTuning updateSuggestedQuestionForFineTuning(
      @PathVariable("suggestedQuestion") @Schema(type = "integer")
          SuggestedQuestionForFineTuning suggestedQuestion,
      @RequestBody QuestionSuggestionParams suggestion)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization();
    return suggestedQuestionForFineTuningService.update(suggestedQuestion, suggestion);
  }

  @PostMapping("/{suggestedQuestion}/duplicate")
  @Transactional
  public SuggestedQuestionForFineTuning duplicate(
      @PathVariable("suggestedQuestion") @Schema(type = "integer")
          SuggestedQuestionForFineTuning suggestedQuestion)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization();
    return suggestedQuestionForFineTuningService.duplicate(suggestedQuestion);
  }

  @PostMapping("/{suggestedQuestion}/delete")
  @Transactional
  public SuggestedQuestionForFineTuning delete(
      @PathVariable("suggestedQuestion") @Schema(type = "integer")
          SuggestedQuestionForFineTuning suggestedQuestion)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization();
    return suggestedQuestionForFineTuningService.delete(suggestedQuestion);
  }

  @PostMapping("/upload-and-trigger-fine-tuning")
  @Transactional
  public void uploadAndTriggerFineTuning() throws UnexpectedNoAccessRightException, IOException {
    authorizationService.assertAdminAuthorization();
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
    authorizationService.assertAdminAuthorization();
    return fineTuningService.getSuggestedQuestionForFineTunings();
  }
}
