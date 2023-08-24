package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.ChatRequest;
import com.odde.doughnut.entities.json.ChatResponse;
import com.odde.doughnut.services.ChatService;
import com.theokanning.openai.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/v1")
public class RestChatController {

  private final ChatService chatService;

  public RestChatController(@Qualifier("testableOpenAiApi") OpenAiApi openAiApi) {
    this.chatService = new ChatService(openAiApi);
  }

  @PostMapping("/chat")
  public ChatResponse chat(@RequestBody ChatRequest request) {
    String question = request.getAsk();
    String answer = this.chatService.askChatGPT(question);
    return new ChatResponse(answer);
  }
}
