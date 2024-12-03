package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

public class RecallStatus {
  @NotNull public int toRepeatCount;
  @NotNull public int learntCount;
  @Getter @Setter private Timestamp recallWindowEndAt;
}
