package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PromotePointRequestDTO {
  public enum PromotionType {
    CHILD,
    SIBLING;

    public Integer getParentNoteId(Note note) {
      return this == SIBLING ? note.getParent().getId() : note.getId();
    }
  }

  private String point;

  @NotNull private PromotionType promotionType;
}
