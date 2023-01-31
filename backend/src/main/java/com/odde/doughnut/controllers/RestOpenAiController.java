package com.odde.doughnut.controllers;

import com.odde.doughnut.services.MyOpenAiService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/OpenAi")
public class RestOpenAiController {
  private final MyOpenAiService openAiService;

  public RestOpenAiController(MyOpenAiService openAiService) {
    this.openAiService = openAiService;
  }
}
