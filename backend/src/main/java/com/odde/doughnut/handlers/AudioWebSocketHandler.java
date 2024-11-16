package com.odde.doughnut.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AudioUploadDTO;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.TextFromAudioWithCallInfo;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

public class AudioWebSocketHandler extends BinaryWebSocketHandler {

  private final OtherAiServices otherAiServices;
  private final ModelFactoryService modelFactoryService;
  private final ObjectMapper objectMapper;

  @Autowired
  public AudioWebSocketHandler(
      OtherAiServices otherAiServices,
      ModelFactoryService modelFactoryService,
      ObjectMapper objectMapper) {
    this.otherAiServices = otherAiServices;
    this.modelFactoryService = modelFactoryService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
      throws IOException {
    ByteBuffer buffer = message.getPayload();
    byte[] payload = new byte[buffer.remaining()];
    buffer.get(payload);

    AudioUploadDTO audioUploadDTO = objectMapper.readValue(payload, AudioUploadDTO.class);

    Optional<TextFromAudioWithCallInfo> result =
        otherAiServices.getTextFromAudio(
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
