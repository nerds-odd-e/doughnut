package com.odde.doughnut.services;

import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
  @Autowired private OpenAiApiHandler openAiApiHandler;

  public String askChatGPT(String askStatement) {
    return openAiApiHandler.getOpenAiAnswer(askStatement);
  }
}
