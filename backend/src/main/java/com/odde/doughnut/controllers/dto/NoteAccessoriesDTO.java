package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.validators.ValidateNotePicture;
import com.odde.doughnut.models.ImageBuilder;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@ValidateNotePicture
public class NoteAccessoriesDTO {
  @Getter @Setter private String url;

  @Getter @Setter private String pictureUrl;

  @Pattern(
      regexp = "^(((-?[0-9.]+\\s+){3}-?[0-9.]+\\s+)*((-?[0-9.]+\\s+){3}-?[0-9.]+))?$",
      message = "must be 'x y width height [x y width height...]'")
  @Getter
  @Setter
  private String pictureMask;

  @Getter @Setter private Boolean useParentPicture = false;

  @Getter @Setter private MultipartFile uploadPictureProxy;

  @Getter @Setter private MultipartFile attachAudioProxy;

  public Image fetchUploadedPicture(User user) throws IOException {
    MultipartFile file = getUploadPictureProxy();
    if (file != null && !file.isEmpty()) {
      return new ImageBuilder().buildImageFromUploadedPicture(user, file);
    }
    return null;
  }

  public Audio fetchAttachAudio(User user) throws IOException {
    MultipartFile file = getAttachAudioProxy();
    if (file != null && !file.isEmpty()) {
      return null;
    }
    return null;
  }
}
