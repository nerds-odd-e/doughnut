package com.odde.doughnut.services;

import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

  private final OpenAiApiHandler openAiApiHandler;

  public ChatService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String askChatGPT(String askStatement) {
    return openAiApiHandler.getOpenAiAnswer(askStatement);
  }
}
