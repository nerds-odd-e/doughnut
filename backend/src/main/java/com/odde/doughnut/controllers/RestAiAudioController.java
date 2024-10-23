package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.TextFromAudio;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class RestAiAudioController {

  private final AiAdvisorService aiAdvisorService;
  private final ModelFactoryService modelFactoryService;

  public RestAiAudioController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService) {
    this.aiAdvisorService = new AiAdvisorService(openAiApi);
    this.modelFactoryService = modelFactoryService;
  }

  @PatchMapping(path = "/{note}/audio-to-srt")
  @Transactional
  public Optional<TextFromAudio> convertNoteAudioToSRT(
      @PathVariable(name = "note") @Schema(type = "integer") Note note) throws IOException {
    Audio audio = note.getNoteAccessory().getAudioAttachment();
    return aiAdvisorService
        .getOtherAiServices()
        .getTextFromAudio(
            audio.getName(),
            audio.getBlob().getData(),
            getGlobalSettingsService().globalSettingOthers().getValue());
  }

  @PostMapping(
      path = "/convertSrt",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public Optional<TextFromAudio> convertSrt(@Valid @ModelAttribute AudioUploadDTO audioFile)
      throws IOException {
    String filename = audioFile.getUploadAudioFile().getOriginalFilename();
    byte[] bytes = audioFile.getUploadAudioFile().getBytes();
    return aiAdvisorService
        .getOtherAiServices()
        .getTextFromAudio(
            filename, bytes, getGlobalSettingsService().globalSettingOthers().getValue());
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }
}
