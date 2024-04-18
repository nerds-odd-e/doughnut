package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public class AudioUploadDTO {
  @Getter @Setter private MultipartFile uploadAudioFile;
}
