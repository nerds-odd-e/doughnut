package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.validators.ValidateMultipartFile;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AudioUploadDTO {
  @ValidateMultipartFile(
      maxSize = 20 * 1024 * 1024,
      allowedTypes = {
        "audio/mpeg",
        "audio/wav",
        "audio/mp4",
        "audio/webm",
        "audio/webm;codecs=opus",
        "audio/m4a"
      })
  private MultipartFile uploadAudioFile;

  private byte[] audioData;
  private String additionalProcessingInstructions;
  private String threadId;
  private String runId;
  private String toolCallId;

  @JsonProperty("isMidSpeech")
  private boolean isMidSpeech;

  private String previousNoteDetailsToAppendTo;
}
