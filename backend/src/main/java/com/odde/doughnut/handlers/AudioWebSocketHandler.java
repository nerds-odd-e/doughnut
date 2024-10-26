package com.odde.doughnut.handlers;

import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.TextFromAudio;
import com.theokanning.openai.client.OpenAiApi;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

public class AudioWebSocketHandler extends BinaryWebSocketHandler {

  private final AiAdvisorService aiAdvisorService;
  private final ModelFactoryService modelFactoryService;

  @Autowired
  public AudioWebSocketHandler(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
    this.aiAdvisorService = new AiAdvisorService(openAiApi);
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
      throws IOException {
    byte[] audioData = message.getPayload().array();

    Optional<TextFromAudio> result =
        aiAdvisorService
            .getOtherAiServices()
            .getTextFromAudio(
                getPreviousNoteDetails(session),
                "stream.wav",
                audioData,
                getGlobalSettingsService().globalSettingOthers().getValue());

    if (result.isPresent()) {
      session.sendMessage(new TextMessage(result.get().getCompletionMarkdownFromAudio()));
    }
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }

  private String getPreviousNoteDetails(WebSocketSession session) {
    // Implement logic to get previous note details from session
    return "";
  }
}
