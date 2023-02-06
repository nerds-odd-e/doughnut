package com.odde.doughnut.services;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import retrofit2.HttpException;

@Service
public class OpenAiWrapperService {
  private final OpenAiService service;
  private final String textModel = "text-davinci-003";

  public OpenAiWrapperService(OpenAiService openAiService) {
    service = openAiService;
  }

  private String getOpenAiResponse(String prompt) {
    CompletionRequest completionRequest =
        CompletionRequest.builder().prompt(prompt).model(textModel).echo(true).build();
    var choices = service.createCompletion(completionRequest).getChoices();
    return choices.stream().map(CompletionChoice::getText).collect(Collectors.joining(""));
  }

  public String getDescription(String item) {
    try {
      String prompt = "Tell me about " + item + ".";
      return getOpenAiResponse(prompt).replace(prompt, "").trim();
    } catch (HttpException e) {
      return "";
    }
  }
}
