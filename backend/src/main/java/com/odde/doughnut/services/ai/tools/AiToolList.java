package com.odde.doughnut.services.ai.tools;

import static com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder.askClarificationQuestion;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.odde.doughnut.controllers.json.ClarifyingQuestionAndAnswer;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.FunctionExecutor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;

public class AiToolList {
  private final FunctionExecutor functionExecutor;

  public AiToolList(FunctionExecutor functionExecutor) {

    this.functionExecutor = functionExecutor;
  }

  public Collection<ChatFunction> getFunctions() {
    return functionExecutor.getFunctions();
  }

  @AllArgsConstructor
  public static class UserResponseToClarifyingQuestion {
    public String answerFromUser;
  }

  public List<ChatMessage> functionReturningMessages(ClarifyingQuestionAndAnswer qa) {
    ChatMessage functionCallMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value());
    functionCallMessage.setFunctionCall(
        new ChatFunctionCall(
            askClarificationQuestion, defaultObjectMapper().valueToTree(qa.questionFromAI)));
    Optional<ChatMessage> functionCallResponse =
        execute(
            functionCallMessage.getFunctionCall(),
            w -> new UserResponseToClarifyingQuestion(qa.answerFromUser));
    return List.of(functionCallMessage, functionCallResponse.get());
  }

  private Optional<ChatMessage> execute(
      ChatFunctionCall functionCall, Function<Object, Object> executor) {
    // The API design of FunctionExecutor get an executor at the beginning, weather it is used or
    // not.
    // We choose to set the executor here, so it is only used when it is needed.
    functionExecutor.getFunctions().stream()
        .filter(f -> f.getName().equals(functionCall.getName()))
        .findFirst()
        .ifPresent(
            f -> {
              f.setExecutor(executor);
            });
    return functionExecutor.executeAndConvertToMessageSafely(functionCall);
  }
}
