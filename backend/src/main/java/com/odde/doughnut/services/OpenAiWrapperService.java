package com.odde.doughnut.services;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.HttpException;

@Service
public class OpenAiWrapperService {
  @Value("${spring.openai.token}")
  private String openAiToken;

  private String textModel = "text-davinci-003";

  private String getOpenAiResponse(String prompt) {
    var service = new OpenAiService(openAiToken);
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
