package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.ChatRequest;
import com.odde.doughnut.entities.json.ChatResponse;
import com.odde.doughnut.services.AiAdvisorService;
import com.theokanning.openai.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestChatController {

  private final AiAdvisorService aiAdvisorService;

  public RestChatController(@Qualifier("testableOpenAiApi") OpenAiApi openAiApi) {
    this.aiAdvisorService = new AiAdvisorService(openAiApi);
  }

  @PostMapping("/chat")
  public ChatResponse chat(@RequestBody ChatRequest request) {
    String question = request.getQuestion();
    String answer = this.aiAdvisorService.chatToAi(question);
    return new ChatResponse(answer);
  }
}
