package com.odde.donut.controllers.dto;

import com.odde.donut.validators.ValidateMultipartFile;
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

  private boolean midSpeech;
}
