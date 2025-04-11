package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.theokanning.openai.assistants.message.MessageRequest;
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
    return generateQuestionWithChatCompletion(additionalMessage);
  }

  private MCQWithAnswer generateQuestionWithChatCompletion(MessageRequest additionalMessage) {
    var chatRequestBuilder = getChatRequestBuilder();

    String instructions = notebookAssistantForNoteService.note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addSystemMessage(instructions);
    }

    // Add the question generation instruction
    chatRequestBuilder.addUserMessage(AiToolFactory.mcqWithAnswerAiTool().getMessageBody());

    // Add any additional message if provided
    if (additionalMessage != null) {
      chatRequestBuilder.addUserMessage(additionalMessage.getContent().toString());
    }

    return notebookAssistantForNoteService
        .getOpenAiApiHandler()
        .requestAndGetJsonSchemaResult(AiToolFactory.mcqWithAnswerAiTool(), chatRequestBuilder)
        .map(
            jsonNode -> {
              try {
                MCQWithAnswer question =
                    new ObjectMapper().treeToValue(jsonNode, MCQWithAnswer.class);

                // Validate the question
                if (question != null
                    && question.getMultipleChoicesQuestion().getStem() != null
                    && !Strings.isBlank(question.getMultipleChoicesQuestion().getStem())) {
                  // Validate the correct choice index is within bounds
                  int correctChoiceIndex = question.getCorrectChoiceIndex();
                  int choicesCount = question.getMultipleChoicesQuestion().getChoices().size();
                  if (correctChoiceIndex < 0 || correctChoiceIndex >= choicesCount) {
                    return null; // Reject questions with invalid choice indices
                  }
                  return question;
                }
                return null;
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .orElse(null);
  }

  public Optional<QuestionEvaluation> evaluateQuestion(MCQWithAnswer question)
      throws JsonProcessingException {
    return evaluateQuestionWithChatCompletion(question);
  }

  private Optional<QuestionEvaluation> evaluateQuestionWithChatCompletion(MCQWithAnswer question) {
    var chatRequestBuilder = getChatRequestBuilder();

    String instructions = notebookAssistantForNoteService.note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addSystemMessage(instructions);
    }

    Optional<JsonNode> result =
        notebookAssistantForNoteService
            .getOpenAiApiHandler()
            .requestAndGetJsonSchemaResult(
                AiToolFactory.questionEvaluationAiTool(question), chatRequestBuilder);

    if (result.isEmpty()) {
      throw new RuntimeException("Failed to evaluate question: No valid response from API");
    }

    return result.map(
        jsonNode -> {
          try {
            return new ObjectMapper().treeToValue(jsonNode, QuestionEvaluation.class);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private OpenAIChatRequestBuilder getChatRequestBuilder() {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    String noteDescription = notebookAssistantForNoteService.note.getGraphRAGDescription();
    return new OpenAIChatRequestBuilder().model(modelName).addSystemMessage(noteDescription);
  }
}
