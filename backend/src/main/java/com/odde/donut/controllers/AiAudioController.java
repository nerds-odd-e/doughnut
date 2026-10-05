package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.*;
import com.odde.donut.services.SRTProcessor;
import com.odde.donut.services.ai.OtherAiServices;
import com.odde.donut.services.ai.TextFromAudioWithCallInfo;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audio")
class AiAudioController {

  private final OtherAiServices otherAiServices;

  @Autowired
  public AiAudioController(OtherAiServices otherAiServices) {
    this.otherAiServices = otherAiServices;
  }

  @PostMapping(
      path = "/audio-to-text",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public TextFromAudioWithCallInfo audioToText(@Valid @ModelAttribute AudioUploadDTO audioFile)
      throws IOException {
    String filename = audioFile.getUploadAudioFile().getOriginalFilename();
    byte[] bytes = audioFile.getUploadAudioFile().getBytes();
    String transcriptionFromAudio = otherAiServices.getTranscriptionFromAudio(filename, bytes);

    SRTProcessor.SRTProcessingResult processedResult =
        new SRTProcessor().process(transcriptionFromAudio, audioFile.isMidSpeech());

    TextFromAudioWithCallInfo textFromAudioWithCallInfo = new TextFromAudioWithCallInfo();
    textFromAudioWithCallInfo.setSegmentTexts(processedResult.getSegmentTexts());
    textFromAudioWithCallInfo.setEndTimestamp(processedResult.getEndTimestamp());
    textFromAudioWithCallInfo.setRawSRT(processedResult.getProcessedSRT());
    return textFromAudioWithCallInfo;
  }
}
