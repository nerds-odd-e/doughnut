package com.odde.doughnut.entities.json;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class CommentCreation {
  @Getter @Setter @Valid
  @NotNull private Integer note_id;

  @Getter @Setter @Valid
  @NotNull private String text;
}
