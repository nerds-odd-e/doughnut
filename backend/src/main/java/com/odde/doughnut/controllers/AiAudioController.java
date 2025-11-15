package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.SRTProcessor;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.TextFromAudioWithCallInfo;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/audio")
class AiAudioController {

  OtherAiServices otherAiServices;
  private final ModelFactoryService modelFactoryService;

  public AiAudioController(
      OtherAiServices otherAiServices, ModelFactoryService modelFactoryService) {
    this.otherAiServices = otherAiServices;
    this.modelFactoryService = modelFactoryService;
  }

  @PostMapping(
      path = "/audio-to-text",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public Optional<TextFromAudioWithCallInfo> audioToText(
      @Valid @ModelAttribute AudioUploadDTO audioFile) throws IOException {
    String filename = audioFile.getUploadAudioFile().getOriginalFilename();
    byte[] bytes = audioFile.getUploadAudioFile().getBytes();
    String transcriptionFromAudio = otherAiServices.getTranscriptionFromAudio(filename, bytes);

    SRTProcessor srtProcessor = new SRTProcessor();
    SRTProcessor.SRTProcessingResult processedResult =
        srtProcessor.process(transcriptionFromAudio, audioFile.isMidSpeech());

    return otherAiServices
        .getTextFromAudio(
            getGlobalSettingsService().globalSettingOthers().getValue(),
            processedResult.getProcessedSRT(),
            audioFile.getAdditionalProcessingInstructions(),
            audioFile.getPreviousNoteDetailsToAppendTo())
        .map(
            noteDetailsCompletion -> {
              TextFromAudioWithCallInfo textFromAudioWithCallInfo = new TextFromAudioWithCallInfo();
              textFromAudioWithCallInfo.setCompletionFromAudio(noteDetailsCompletion);
              textFromAudioWithCallInfo.setEndTimestamp(processedResult.getEndTimestamp());
              textFromAudioWithCallInfo.setRawSRT(processedResult.getProcessedSRT());
              return textFromAudioWithCallInfo;
            });
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }
}
