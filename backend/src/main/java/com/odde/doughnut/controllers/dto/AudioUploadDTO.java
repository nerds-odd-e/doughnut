package com.odde.doughnut.controllers.dto;

import static java.util.Objects.requireNonNull;

import com.odde.doughnut.validators.ValidAudioFile;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public class AudioUploadDTO {
  @ValidAudioFile(
      maxSize = 20 * 1024 * 1024,
      allowedTypes = {"audio/mpeg", "audio/wav", "audio/mp4"})
  @Getter
  @Setter
  private MultipartFile uploadAudioFile;

  public void validate() throws Exception {
    String filename = getUploadAudioFile().getOriginalFilename();
    String filename1 = requireNonNull(filename);
    if (!(filename1.endsWith(".mp3") || filename1.endsWith(".m4a") || filename1.endsWith(".wav"))) {
      throw new Exception("Invalid format");
    }
    if (getUploadAudioFile().getSize() >= 1024 * 1024 * 20) {
      throw new Exception("Size Exceeded");
    }
  }
}
