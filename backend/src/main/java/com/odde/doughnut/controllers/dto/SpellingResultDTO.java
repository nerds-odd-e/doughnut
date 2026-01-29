package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class SpellingResultDTO {
  @NotNull @Getter private final Note note;
  @Getter private final String answer;
  @Getter private final Boolean isCorrect;
  @Getter private final Integer memoryTrackerId;
  @Getter private final Boolean thresholdExceeded;

  public SpellingResultDTO(
      Note note,
      String answer,
      Boolean isCorrect,
      Integer memoryTrackerId,
      Boolean thresholdExceeded) {
    this.note = note;
    this.answer = answer;
    this.isCorrect = isCorrect;
    this.memoryTrackerId = memoryTrackerId;
    this.thresholdExceeded = thresholdExceeded;
  }
}
