package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.validators.ValidateNotePicture;
import com.odde.doughnut.models.ImageBuilder;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class AudioUploadDTO {
  @Getter @Setter private MultipartFile uploadAudioFile;
}
