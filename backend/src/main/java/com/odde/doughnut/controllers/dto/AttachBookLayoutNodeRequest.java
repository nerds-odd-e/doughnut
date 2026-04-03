package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachBookLayoutNodeRequest {

  @NotBlank
  @Size(max = 512)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String title;

  @NotNull
  @Valid
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private AttachBookAnchorRequest startAnchor;

  @NotNull
  @Valid
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private AttachBookAnchorRequest endAnchor;

  @Valid private List<AttachBookLayoutNodeRequest> children;
}
