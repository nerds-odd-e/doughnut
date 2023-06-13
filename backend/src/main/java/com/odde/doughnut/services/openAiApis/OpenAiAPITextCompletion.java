package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import java.util.List;

public class OpenAiAPITextCompletion extends OpenAiApiHandlerBase {

  private OpenAiApi openAiApi;
  public static final String OPEN_AI_MODEL = "text-davinci-003";

  public OpenAiAPITextCompletion(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  private List<ChatCompletionChoice> getChatCompletionChoices(
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
  }

  public AiSuggestion getOpenAiCompletion(String prompt) {
    System.out.println("2 getOpenAiCompletion prompt:" + prompt);
    return withExceptionHandler(
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
  }

  private static ChatCompletionRequest getChatCompletionRequest(String prompt) {
    System.out.println("4 prompt:" + prompt);
    List<ChatMessage> messages = new ArrayList<>();
    final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);
    messages.add(0, systemMessage);

    return ChatCompletionRequest.builder()
        .model(OPEN_AI_MODEL)
        // This can go higher (up to 4000 - prompt size), but openAI performance goes down
        // https://help.openai.com/en/articles/4936856-what-are-tokens-and-how-to-count-them
        .maxTokens(50)
        //
        // an effort has been made the response more responsive by using stream(true)
        // how every, due to the library limitation, we cannot do it yet.
        // find more details here:
        //     https://github.com/TheoKanning/openai-java/issues/83
        .stream(false)
        .echo(true)
        .build();
  }
}
