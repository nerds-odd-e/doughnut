package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.utils.ImageBuilder;
import com.odde.doughnut.validators.ValidateNoteImage;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@ValidateNoteImage
public class NoteAccessoriesDTO {
  @Getter @Setter private String url;

  @Getter @Setter private String imageUrl;

  @Pattern(
      regexp = "^(((-?[0-9.]+\\s+){3}-?[0-9.]+\\s+)*((-?[0-9.]+\\s+){3}-?[0-9.]+))?$",
      message = "must be 'x y width height [x y width height...]'")
  @Getter
  @Setter
  private String imageMask;

  @Getter @Setter private Boolean useParentImage = false;

  @Getter @Setter private MultipartFile uploadImage;

  public Image fetchUploadedImage(User user) throws IOException {
    MultipartFile file = getUploadImage();
    if (file != null && !file.isEmpty()) {
      return new ImageBuilder().buildImageFromUploadedImage(user, file);
    }
    return null;
  }
}
