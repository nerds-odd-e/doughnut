package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachBookLayoutNodeRequest {

  @NotBlank
  @Size(max = 512)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String title;

  @Valid
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private AttachBookAnchorRequest startAnchor;

  @Valid private List<AttachBookLayoutNodeRequest> children;

  private List<Map<String, Object>> contentBlocks;
}
