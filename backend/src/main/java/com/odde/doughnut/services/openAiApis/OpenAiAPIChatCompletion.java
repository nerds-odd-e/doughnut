package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

public class OpenAiAPIChatCompletion extends OpenAiApiHandlerBase {

  private final OpenAiApi openAiApi;

  public OpenAiAPIChatCompletion(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public AiSuggestion getOpenAiCompletion(List<ChatMessage> chatMessages, int maxTokens) {
    return getAiSuggestion(
        defaultChatCompletionRequestBuilder(chatMessages).maxTokens(maxTokens).build());
  }

  public class AIQuestionOption {
    @JsonPropertyDescription("The option to ask the user")
    public String option;

    @JsonPropertyDescription("Whether the option is correct or not")
    @JsonProperty(required = true)
    public Boolean correct;
  }

  public class AIGeneratedQuestion {
    @JsonPropertyDescription("The question to ask the user")
    @JsonProperty(required = true)
    public String question;

    @JsonPropertyDescription("The options to ask the user to choose from")
    @JsonProperty(required = true)
    public List<AIQuestionOption> options;
  }

  public AiSuggestion getOpenAiCompletion1(List<ChatMessage> chatMessages, int maxTokens) {
    ChatFunction build =
        ChatFunction.builder()
            .name("ask_single_answer_multiple_choice_question")
            .description("Ask a single-answer multiple-choice question to the user")
            .executor(AIGeneratedQuestion.class, null)
            .build();

    return getAiSuggestion(
        defaultChatCompletionRequestBuilder(chatMessages)
            .functions(List.of(build))
            .functionCall(new ChatCompletionRequest.ChatCompletionRequestFunctionCall("none"))
            .maxTokens(maxTokens)
            .build());
  }

  private AiSuggestion getAiSuggestion(ChatCompletionRequest request) {
    return withExceptionHandler(
        () ->
            openAiApi.createChatCompletion(request).blockingGet().getChoices().stream()
                .findFirst()
                .map(AiSuggestion::from)
                .orElse(null));
  }

  private static ChatCompletionRequest.ChatCompletionRequestBuilder
      defaultChatCompletionRequestBuilder(List<ChatMessage> messages) {
    return ChatCompletionRequest.builder()
        .model("gpt-3.5-turbo")
        .messages(messages)
        //
        // an effort has been made to make the api call more responsive by using stream(true)
        // however, due to the library limitation, we cannot do it yet.
        // find more details here:
        //    https://github.com/TheoKanning/openai-java/issues/83
        .stream(false)
        .n(1);
  }
}
