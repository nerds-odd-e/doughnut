package com.odde.doughnut.controllers.dto;

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
  private String title;

  @Valid private List<AttachBookLayoutNodeRequest> children;

  private List<Map<String, Object>> contentBlocks;
}
