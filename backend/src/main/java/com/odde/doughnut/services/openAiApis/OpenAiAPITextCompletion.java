package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpenAiAPITextCompletion extends OpenAiApiHandlerBase {

  private OpenAiApi openAiApi;
  private OpenAiService service = new OpenAiService(System.getenv("OPENAI_API_TOKEN"));
  public static final String OPEN_AI_MODEL = "gpt-3.5-turbo";

  public OpenAiAPITextCompletion(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  private List<ChatCompletionChoice> getChatCompletionChoices(
<<<<<<< HEAD
      ChatCompletionRequest completionRequest) {

    return openAiApi
        .createChatCompletion(completionRequest)
        .doOnError(Throwable::printStackTrace)
        .blockingGet()
        .getChoices();
||||||| parent of adbd859e8 (debug)
      ChatCompletionRequest completionRequest) {
    System.out.println("1 completetionRequest:" + completionRequest);
    List<ChatCompletionChoice> list = openAiApi
        .createChatCompletion(completionRequest)
        .doOnError(Throwable::printStackTrace)
        .blockingGet()
        .getChoices();
    System.out.print("5  ");
    System.out.println(list);
    return list;
=======
    ChatCompletionRequest completionRequest) {
    return openAiApi.createChatCompletion(completionRequest).blockingGet().getChoices();

>>>>>>> adbd859e8 (debug)
  }

  public AiSuggestion getOpenAiCompletion(String prompt) {
<<<<<<< HEAD
||||||| parent of adbd859e8 (debug)
    System.out.println("2 getOpenAiCompletion prompt:" + prompt);
=======

>>>>>>> adbd859e8 (debug)
    return withExceptionHandler(
<<<<<<< HEAD
        () -> {
          ChatCompletionRequest completionRequest = getChatCompletionRequest(prompt);
          List<ChatCompletionChoice> choices = getChatCompletionChoices(completionRequest);
          return choices.stream()
              .findFirst()
              .map(
                  chatCompletionChoice ->
                      new AiSuggestion(
                          chatCompletionChoice.getMessage().getContent(),
                          chatCompletionChoice.getFinishReason()))
              .orElse(null);
        });
||||||| parent of adbd859e8 (debug)
        () -> {
          ChatCompletionRequest completionRequest = getChatCompletionRequest(prompt);
          System.out.println("3 getOpenAiCompletion completionRequest:" +  completionRequest);
          List<ChatCompletionChoice> choices = getChatCompletionChoices(completionRequest);
          return choices.stream()
              .findFirst()
              .map(
                  chatCompletionChoice -> new AiSuggestion(
                      chatCompletionChoice.getMessage().getContent(),
                      chatCompletionChoice.getFinishReason()))
              .orElse(null);
        });
=======
      () -> {
        ChatCompletionRequest completionRequest = getChatCompletionRequest(prompt);
        List<ChatCompletionChoice> choices = getChatCompletionChoices(completionRequest);
        return choices.stream()
          .findFirst()
          .map(
            chatCompletionChoice -> new AiSuggestion(
              chatCompletionChoice.getMessage().getContent(),
              chatCompletionChoice.getFinishReason()))
          .orElse(null);
      });
>>>>>>> adbd859e8 (debug)
  }

  private static ChatCompletionRequest getChatCompletionRequest(String prompt) {
    List<ChatMessage> messages = new ArrayList<>();
    final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);
    messages.add(0, systemMessage);

    return ChatCompletionRequest.builder()
      .model(OPEN_AI_MODEL)
      .messages(messages)
      .n(1)
      .maxTokens(50)
      .logitBias(new HashMap<>())
      .build();
  }
}
