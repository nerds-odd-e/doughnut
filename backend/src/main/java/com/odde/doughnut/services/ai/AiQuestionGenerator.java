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

public class AiQuestionGenerator {
  private final Note note;

  private final OpenAiApiHandler openAiApiHandler;

  public AiQuestionGenerator(Note note, OpenAiApiHandler openAiApiHandler) {
    this.note = note;
    this.openAiApiHandler = openAiApiHandler;
  }

  public AIGeneratedQuestion getAiGeneratedQuestion() throws QuizQuestionNotPossibleException {
    try {
      final AIGeneratedQuestion question = getValidQuestion(false);
      if (question.confidence > 9) return question;
      if (questionMakeSense(question)) {
        question.stem += " (confidence: " + question.confidence + ". Reevaluated.)";
        return question;
      }
    } catch (QuizQuestionNotPossibleException e) {
    }
    final AIGeneratedQuestion gpt4question = getValidQuestion(true);
    gpt4question.stem += " (GPT-4)";
    return gpt4question;
  }

  private Boolean questionMakeSense(AIGeneratedQuestion question) {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .contentOfNoteOfCurrentFocus(note)
            .validateQuestionAgain(question)
            .maxTokens(1500)
            .build();

    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .flatMap(QuestionEvaluation::getQuestionEvaluation)
        .map(eq -> eq.makeSense(question.correctChoiceIndex))
        .orElse(false);
  }

  private AIGeneratedQuestion getValidQuestion(boolean useGPT4)
      throws QuizQuestionNotPossibleException {
    JsonNode question = useGPT4 ? generateQuestionByGPT4() : generateQuestionByGPT3_5();
    return AIGeneratedQuestion.getValidQuestion(question);
  }

  private JsonNode generateQuestionByGPT3_5() throws QuizQuestionNotPossibleException {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .contentOfNoteOfCurrentFocus(note)
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

  private JsonNode generateQuestionByGPT4() throws QuizQuestionNotPossibleException {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .contentOfNoteOfCurrentFocus(note)
            .userInstructionToGenerateQuestionWithFunctionCall()
            .maxTokens(1500)
            .useGPT4()
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
