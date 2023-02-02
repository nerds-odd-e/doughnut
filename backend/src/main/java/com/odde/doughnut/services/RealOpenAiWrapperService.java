package com.odde.doughnut.services;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("RealOpenAiWrapperService")
public class RealOpenAiWrapperService implements OpenAiWrapperService {
  @Value("${spring.openai.token}")
  private String OpenAiToken;

  private String textModel = "text-davinci-003";

  private String getOpenAiResponse(String prompt) {
    var service = new OpenAiService(OpenAiToken);
    CompletionRequest completionRequest =
        CompletionRequest.builder().prompt(prompt).model(textModel).echo(true).build();
    var choices = service.createCompletion(completionRequest).getChoices();
    return choices.stream().map(CompletionChoice::getText).collect(Collectors.joining(""));
  }

  public String getDescription(String item) {
    String prompt = "What is " + item;
    return getOpenAiResponse(prompt);
  }
}
