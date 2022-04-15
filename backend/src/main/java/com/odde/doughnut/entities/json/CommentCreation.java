package com.odde.doughnut.entities.json;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class CommentCreation {
  @Getter @Setter @Valid @NotNull public String description;
}
