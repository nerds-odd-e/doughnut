package com.odde.doughnut.controllers;

import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MyOpenAiService;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/OpenAi")
public class RestOpenAiController {

  @PostMapping(value = "/{title}/openai")
  @Transactional
  public String getOpenAiResponse(@PathVariable(name = "title") String title)
    throws BindException, UnexpectedNoAccessRightException {
    OpenAiService service = new OpenAiService("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");
    CompletionRequest completionRequest =
      CompletionRequest.builder().prompt(title).model("davinci").echo(true).build();
    return service.createCompletion(completionRequest).getChoices().get(0).getText();
  }
}
