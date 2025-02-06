package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.theokanning.openai.assistants.message.MessageRequest;
import java.util.ArrayList;
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

  public MCQWithAnswer generateQuestion(MessageRequest additionalMessage)
      throws JsonProcessingException {
    List<MessageRequest> messages = new ArrayList<>();
    messages.add(
        MessageRequest.builder()
            .role("user")
            .content(AiToolFactory.mcqWithAnswerAiTool().getMessageBody())
            .build());

    if (additionalMessage != null) {
      messages.add(additionalMessage);
    }

    MCQWithAnswer question =
        notebookAssistantForNoteService
            .createThreadWithNoteInfo1(messages)
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

  public MCQWithAnswer reGenerateQuestion(
      PredefinedQuestion oldQuestion, QuestionContestResult contestResult)
      throws JsonProcessingException {
    return generateQuestion(
        MessageRequest.builder()
            .role("user")
            .content(
                """
                  Previously generated non-feasible question:
                  %s

                  Non-feasible reason:
                  %s

                  Please regenerate or refine the question based on the above feedback."""
                    .formatted(
                        new ObjectMapper()
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(oldQuestion.getMcqWithAnswer()),
                        contestResult.reason))
            .build());
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
