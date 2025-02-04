package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.ai.tools.AiTool;
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
    MessageRequest message =
        MessageRequest.builder()
            .role("user")
            .content(AiToolFactory.mcqWithAnswerAiTool().getMessageBody())
            .build();

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
            .withTool(
                new AiTool(
                    "evaluate_question",
                    "answer and evaluate the feasibility of the question",
                    QuestionEvaluation.class))
            .withModelName(globalSettingsService.globalSettingEvaluation().getValue())
            .run()
            .getRunResult()
            .getAssumedToolCallArgument(QuestionEvaluation.class);

    return Optional.ofNullable(evaluation);
  }
}
