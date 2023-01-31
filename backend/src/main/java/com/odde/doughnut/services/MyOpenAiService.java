package com.odde.doughnut.services;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;

public class MyOpenAiService {
  private static final String _apiKey = "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";

  public String getOpenAiResponse(String title) {
    OpenAiService service = new OpenAiService(_apiKey);
    CompletionRequest completionRequest =
        CompletionRequest.builder().prompt(title).model("davinci").echo(true).build();
    return service.createCompletion(completionRequest).getChoices().get(0).getText();
  }
}
