package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.validators.ValidateMultipartFile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Schema(name = "NoteImageUploadDTO")
public class NoteImageUploadDTO {

  @NotNull
  @ValidateMultipartFile(
      maxSize = 10 * 1024 * 1024,
      allowedTypes = {
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/gif",
        "image/webp",
      })
  @Schema(type = "string", format = "binary")
  private MultipartFile uploadImage;
}
