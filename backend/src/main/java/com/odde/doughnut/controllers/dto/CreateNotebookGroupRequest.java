package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNotebookGroupRequest {
  @NotBlank
  @Size(max = 255)
  private String name;
}
