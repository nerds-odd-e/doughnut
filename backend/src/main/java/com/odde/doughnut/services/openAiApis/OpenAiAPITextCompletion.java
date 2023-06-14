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

  private List<CompletionChoice> getCompletionChoices(CompletionRequest completionRequest) {
    return openAiApi.createCompletion(completionRequest).blockingGet().getChoices();
  }

  public AiSuggestion getOpenAiCompletion(String prompt) {
    return withExceptionHandler(
        () -> {
          CompletionRequest completionRequest = getCompletionRequest(prompt);
          List<CompletionChoice> choices = getCompletionChoices(completionRequest);
          return choices.stream()
              .findFirst()
              .map(
                  completionChoice ->
                      new AiSuggestion(
                          completionChoice.getText(), completionChoice.getFinish_reason()))
              .orElse(null);
        });
  }

  private static CompletionRequest getCompletionRequest(String prompt) {
    return CompletionRequest.builder()
        .prompt(prompt)
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
