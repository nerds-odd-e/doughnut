package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SrtDto {
  @NotNull private String srt;
}
