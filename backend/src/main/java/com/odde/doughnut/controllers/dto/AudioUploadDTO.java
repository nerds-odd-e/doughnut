package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.AttachmentBlob;
import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.validators.ValidateMultipartFile;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public class AudioUploadDTO {
  @ValidateMultipartFile(
      maxSize = 20 * 1024 * 1024,
      allowedTypes = {"audio/mpeg", "audio/wav", "audio/mp4"})
  @Getter
  @Setter
  private MultipartFile uploadAudioFile;

  public Audio fetchUploadedAudio() throws IOException {
    Audio audio = new Audio();
    audio.setName(getUploadAudioFile().getOriginalFilename());
    audio.setContentType(getUploadAudioFile().getContentType());

    AttachmentBlob audioBlob = new AttachmentBlob();
    audioBlob.setData(getUploadAudioFile().getBytes());
    audio.setBlob(audioBlob);
    return audio;
  }
}
