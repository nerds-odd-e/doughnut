package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.validators.ValidateMultipartFile;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
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

  private String additionalProcessingInstructions;

  private byte[] audioData;

  private String previousNoteDetails;

  private String threadId;

  private String runId;

  private String toolCallId;
}
