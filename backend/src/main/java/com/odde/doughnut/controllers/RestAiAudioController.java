package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.TextFromAudio;
import com.theokanning.openai.client.OpenAiApi;
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

  @PostMapping(
      path = "/audio-to-text",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public Optional<TextFromAudio> audioToText(@Valid @ModelAttribute AudioUploadDTO audioFile)
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
