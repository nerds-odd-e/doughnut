package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.util.stream.Stream;

public class ChatService {
  public ChatService(OpenAiApiHandler openAiApiHandler) {}

  public static Stream<AiTool> getTools() {
    return Stream.of();
  }
}
