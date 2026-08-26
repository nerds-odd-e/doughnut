package com.odde.donut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachBookLayoutRequest {

  @NotEmpty
  @Valid
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Root nodes of the nested book layout.")
  private List<AttachBookLayoutNodeRequest> roots;
}
