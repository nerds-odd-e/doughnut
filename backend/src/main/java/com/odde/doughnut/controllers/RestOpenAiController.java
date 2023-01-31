package com.odde.doughnut.controllers;

import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/OpenAi")
public class RestOpenAiController {

  @GetMapping(value = "/{title}")
  @Transactional
  public String getOpenAiResponse(@PathVariable(name = "title") String title)
      throws BindException, UnexpectedNoAccessRightException {
    OpenAiService service =
        new OpenAiService("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");
    CompletionRequest completionRequest =
        CompletionRequest.builder().prompt(title).model("text-davinci-003").echo(true).build();

    var choices = service.createCompletion(completionRequest).getChoices();
    System.out.println(choices.size());
    choices.forEach(choice -> System.out.println(choice.getText()));
    return choices.stream().map(CompletionChoice::getText).collect(Collectors.joining(""));
  }
}
