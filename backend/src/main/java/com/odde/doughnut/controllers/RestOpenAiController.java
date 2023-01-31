package com.odde.doughnut.controllers;

import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MyOpenAiService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/OpenAi")
public class RestOpenAiController {
  private final MyOpenAiService openAiService;

  public RestOpenAiController(MyOpenAiService openAiService) {
    this.openAiService = openAiService;
  }

  @PostMapping(value = "/{title}/openai")
  @Transactional
  public String getOpenAiResponse(@PathVariable(name = "title") String title)
      throws BindException, UnexpectedNoAccessRightException {
    return openAiService.getOpenAiResponse(title);
  }
}
