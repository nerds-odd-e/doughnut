package com.odde.doughnut.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AudioUploadDTO;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.TextFromAudio;
import com.theokanning.openai.client.OpenAiApi;
import java.io.IOException;
import java.nio.ByteBuffer;
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
  private final ObjectMapper objectMapper;

  @Autowired
  public AudioWebSocketHandler(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      ObjectMapper objectMapper) {
    this.modelFactoryService = modelFactoryService;
    this.aiAdvisorService = new AiAdvisorService(openAiApi);
    this.objectMapper = objectMapper;
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
      throws IOException {
    ByteBuffer buffer = message.getPayload();
    byte[] payload = new byte[buffer.remaining()];
    buffer.get(payload);

    AudioUploadDTO audioUploadDTO = objectMapper.readValue(payload, AudioUploadDTO.class);

    Optional<TextFromAudio> result =
        aiAdvisorService
            .getOtherAiServices()
            .getTextFromAudio(
                audioUploadDTO.getPreviousNoteDetails(),
                "stream.wav",
                audioUploadDTO.getAudioData(),
                getGlobalSettingsService().globalSettingOthers().getValue());

    if (result.isPresent()) {
      session.sendMessage(new TextMessage(result.get().getCompletionMarkdownFromAudio()));
    }
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }
}
