package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BookRangeReadingRecordWire")
public class BookRangeReadingRecordWire {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String status;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Timestamp completedAt;
}
