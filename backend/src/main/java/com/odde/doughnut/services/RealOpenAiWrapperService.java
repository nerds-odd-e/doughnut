package com.odde.doughnut.services;

import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service("RealOpenAiService")
public class RealOpenAiService implements OpenAiService {
  @Value("${spring.openai.token}")
  private String OpenAiToken;

  public String getOpenAiResponse(String prompt) {
    OpenAiService service = new OpenAiService(OpenAiToken);
    CompletionRequest completionRequest =
      CompletionRequest.builder().prompt(prompt).model("text-davinci-003").echo(true).build();

    var choices = service.createCompletion(completionRequest).getChoices();
    return choices.stream().map(CompletionChoice::getText).collect(Collectors.joining(""));
  }
}
