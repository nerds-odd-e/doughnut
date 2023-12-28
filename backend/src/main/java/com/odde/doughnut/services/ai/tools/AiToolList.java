package com.odde.doughnut.services.ai.tools;

import static com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder.askClarificationQuestion;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.odde.doughnut.controllers.json.ClarifyingQuestionAndAnswer;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.*;
import java.util.function.Function;
import lombok.AllArgsConstructor;

public class AiToolList {
  final Map<String, ChatFunction> FUNCTIONS = new HashMap<>();

  public AiToolList(List<ChatFunction> functions) {
    functions.forEach(f -> this.FUNCTIONS.put(f.getName(), f));
  }

  public Collection<ChatFunction> getFunctions() {
    return new ArrayList<>(FUNCTIONS.values());
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
    ChatMessage functionCallResponse =
        execute(
            functionCallMessage.getFunctionCall(),
            w -> new UserResponseToClarifyingQuestion(qa.answerFromUser));
    return List.of(functionCallMessage, functionCallResponse);
  }

  private ChatMessage execute(ChatFunctionCall functionCall, Function<Object, Object> executor) {
    new ArrayList<>(FUNCTIONS.values())
        .stream()
            .filter(f -> f.getName().equals(functionCall.getName()))
            .findFirst()
            .ifPresent(
                f -> {
                  f.setExecutor(executor);
                });
    return new FunctionExecutor1(FUNCTIONS.get(functionCall.getName()))
        .executeAndConvertToMessage(functionCall);
  }
}
