package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;

public class AssistantThread {
  public String threadId;
  private final OpenAiApiHandler openAiApiHandler;

  public AssistantThread(String threadId, OpenAiApiHandler openAiApiHandler) {
    this.threadId = threadId;
    this.openAiApiHandler = openAiApiHandler;
  }
}
