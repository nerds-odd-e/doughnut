package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Notebook;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

public class SaveCertificateDetails {
  @Getter
  @Setter
  @Schema(type = "integer")
  Notebook notebook;
}
