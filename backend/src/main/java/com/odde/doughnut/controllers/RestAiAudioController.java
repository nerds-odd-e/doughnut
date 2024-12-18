package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.SRTProcessor;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.TextFromAudioWithCallInfo;
import io.swagger.v3.oas.annotations.media.Schema;
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
class RestAiAudioController {

  OtherAiServices otherAiServices;
  private final ModelFactoryService modelFactoryService;
  private final NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory;

  public RestAiAudioController(
      OtherAiServices otherAiServices,
      ModelFactoryService modelFactoryService,
      NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory) {
    this.otherAiServices = otherAiServices;
    this.modelFactoryService = modelFactoryService;
    this.notebookAssistantForNoteServiceFactory = notebookAssistantForNoteServiceFactory;
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
            audioFile.getPreviousContentToAppendTo())
        .map(
            noteDetailsCompletion -> {
              TextFromAudioWithCallInfo textFromAudioWithCallInfo =
                  new TextFromAudioWithCallInfo();
              textFromAudioWithCallInfo.setCompletionFromAudio(noteDetailsCompletion);
              textFromAudioWithCallInfo.setEndTimestamp(processedResult.getEndTimestamp());
              textFromAudioWithCallInfo.setRawSRT(processedResult.getProcessedSRT());
              return textFromAudioWithCallInfo;
            });
  }

  @PostMapping(
      path = "/audio-to-text/{noteId}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public TextFromAudioWithCallInfo audioToTextForNote(
      @PathVariable("noteId") @Schema(type = "integer") Note note,
      @Valid @ModelAttribute AudioUploadDTO audioUpload)
      throws IOException {
    String filename = audioUpload.getUploadAudioFile().getOriginalFilename();
    byte[] bytes = audioUpload.getUploadAudioFile().getBytes();
    String transcriptionFromAudio = otherAiServices.getTranscriptionFromAudio(filename, bytes);
    SRTProcessor srtProcessor = new SRTProcessor();
    SRTProcessor.SRTProcessingResult processedResult =
        srtProcessor.process(transcriptionFromAudio, audioUpload.isMidSpeech());
    TextFromAudioWithCallInfo textFromAudioWithCallInfo =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .audioTranscriptionToArticle(processedResult.getProcessedSRT(), audioUpload);
    textFromAudioWithCallInfo.setRawSRT(processedResult.getProcessedSRT());
    textFromAudioWithCallInfo.setEndTimestamp(processedResult.getEndTimestamp());
    return textFromAudioWithCallInfo;
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }
}
