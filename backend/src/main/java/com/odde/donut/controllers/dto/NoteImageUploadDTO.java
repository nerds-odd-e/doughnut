package com.odde.donut.controllers.dto;

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
  @Schema(type = "string", format = "binary")
  private MultipartFile uploadImage;
}
