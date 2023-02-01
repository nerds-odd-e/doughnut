package com.odde.doughnut.controllers;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/OpenAi")
public class RestOpenAiController {

  @Value("${spring.openai.token}")
  private String OpenAiToken;

  @GetMapping(value = "/{title}")
  @Transactional
  public String getOpenAiResponse(@PathVariable(name = "title") String title) {
    OpenAiService service = new OpenAiService(OpenAiToken);
    CompletionRequest completionRequest =
        CompletionRequest.builder().prompt(title).model("text-davinci-003").echo(true).build();

    var choices = service.createCompletion(completionRequest).getChoices();
    System.out.println(choices.size());
    choices.forEach(choice -> System.out.println(choice.getText()));
    return choices.stream().map(CompletionChoice::getText).collect(Collectors.joining(""));
  }
}
