package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class SpellingResultDTO {
  @NotNull @Getter private final Note note;
  @Getter private final String answer;
  @Getter private final Boolean isCorrect;

  public SpellingResultDTO(Note note, String answer, Boolean isCorrect) {
    this.note = note;
    this.answer = answer;
    this.isCorrect = isCorrect;
  }
}
