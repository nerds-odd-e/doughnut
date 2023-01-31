package com.odde.doughnut.controllers;

import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
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
      CompletionRequest.builder().prompt(title).model("davinci").echo(true).build();
    return service.createCompletion(completionRequest).getChoices().get(0).getText();
  }
}
