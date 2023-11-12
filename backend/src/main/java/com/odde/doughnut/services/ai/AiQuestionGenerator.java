package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.Optional;

public class AiQuestionGenerator {
  private final Note note;

  private final OpenAiApiHandler openAiApiHandler;

  public AiQuestionGenerator(Note note, OpenAiApiHandler openAiApiHandler) {
    this.note = note;
    this.openAiApiHandler = openAiApiHandler;
  }

  public MCQWithAnswer getAiGeneratedQuestion(String modelName)
      throws QuizQuestionNotPossibleException {
    JsonNode question = generateQuestionByGPT3_5(modelName);
    return MCQWithAnswer.getValidQuestion(question);
  }

  public Optional<QuestionEvaluation> evaluateQuestion(MCQWithAnswer question, String modelName) {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .model(modelName)
            .systemBrief()
            .contentOfNoteOfCurrentFocus(note)
            .evaluateQuestion(question)
            .maxTokens(1500)
            .build();

    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .flatMap(QuestionEvaluation::getQuestionEvaluation);
  }

  private JsonNode generateQuestionByGPT3_5(String modelName)
      throws QuizQuestionNotPossibleException {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .model(modelName)
            .systemBrief()
            .contentOfNoteOfCurrentFocus(note)
            .questionSchemaInPlainChat()
            .userInstructionToGenerateQuestionWithGPT35FineTunedModel()
            .maxTokens(1500)
            .build();

    return openAiApiHandler
        .chatCompletion(chatRequest)
        .map(ChatCompletionChoice::getMessage)
        .map(ChatMessage::getContent)
        .map(AiQuestionGenerator::getJsonNode)
        .orElseThrow(QuizQuestionNotPossibleException::new);
  }

  private JsonNode generateQuestionByGPT4(OpenAIChatAboutNoteRequestBuilder chatBuilder)
      throws QuizQuestionNotPossibleException {
    ChatCompletionRequest chatRequest =
        chatBuilder
            .systemBrief()
            .contentOfNoteOfCurrentFocus(note)
            .userInstructionToGenerateQuestionWithFunctionCall()
            .maxTokens(1500)
            .build();
    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .orElseThrow(QuizQuestionNotPossibleException::new);
  }

  private static JsonNode getJsonNode(String content) {
    try {
      return new ObjectMapper().readTree(content);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
