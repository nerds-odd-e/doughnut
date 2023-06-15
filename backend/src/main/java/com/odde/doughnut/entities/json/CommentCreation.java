package com.odde.doughnut.entities.json;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class CommentCreation {
  @Getter @Setter @Valid @NotNull private Integer note_id;

  @Getter @Setter @Valid @NotNull private String text;
}
