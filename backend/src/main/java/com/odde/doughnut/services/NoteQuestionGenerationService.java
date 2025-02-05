package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.theokanning.openai.assistants.message.MessageRequest;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.util.Strings;

public class NoteQuestionGenerationService {
  protected final GlobalSettingsService globalSettingsService;
  private final NotebookAssistantForNoteService notebookAssistantForNoteService;

  public NoteQuestionGenerationService(
      GlobalSettingsService globalSettingsService,
      NotebookAssistantForNoteService notebookAssistantForNoteService) {
    this.globalSettingsService = globalSettingsService;
    this.notebookAssistantForNoteService = notebookAssistantForNoteService;
  }

  public MCQWithAnswer generateQuestion() throws JsonProcessingException {
    return generateQuestion(null, null);
  }

  public MCQWithAnswer generateQuestion(
      PredefinedQuestion oldQuestion, QuestionContestResult contestResult)
      throws JsonProcessingException {
    MessageRequest message =
        MessageRequest.builder()
            .role("user")
            .content(AiToolFactory.mcqWithAnswerAiTool().getMessageBody())
            .build();

    if (oldQuestion != null && contestResult != null) {
      message =
          MessageRequest.builder()
              .role("user")
              .content(
                  AiToolFactory.mcqWithAnswerAiTool().getMessageBody()
                      + "\nPrevious question: "
                      + oldQuestion.getMcqWithAnswer().getMultipleChoicesQuestion().getStem()
                      + "\nContest reason: "
                      + contestResult.reason)
              .build();
    }

    MCQWithAnswer question =
        notebookAssistantForNoteService
            .createThreadWithNoteInfo1(List.of(message))
            .withTool(AiToolFactory.askSingleAnswerMultipleChoiceQuestion())
            //            .withFileSearch()
            .withModelName(globalSettingsService.globalSettingQuestionGeneration().getValue())
            .run()
            .getRunResult()
            .getLastToolCallArgument(MCQWithAnswer.class);
    if (question != null
        && question.getMultipleChoicesQuestion().getStem() != null
        && !Strings.isBlank(question.getMultipleChoicesQuestion().getStem())) {
      return question;
    }
    return null;
  }

  public Optional<QuestionEvaluation> evaluateQuestion(MCQWithAnswer question)
      throws JsonProcessingException {
    MessageRequest message =
        MessageRequest.builder()
            .role("user")
            .content(AiToolFactory.questionEvaluationAiTool(question).getMessageBody())
            .build();

    QuestionEvaluation evaluation =
        notebookAssistantForNoteService
            .createThreadWithNoteInfo1(List.of(message))
            .withTool(AiToolFactory.evaluateQuestion())
            .withModelName(globalSettingsService.globalSettingEvaluation().getValue())
            .run()
            .getRunResult()
            .getAssumedToolCallArgument(QuestionEvaluation.class);

    return Optional.ofNullable(evaluation);
  }
}
