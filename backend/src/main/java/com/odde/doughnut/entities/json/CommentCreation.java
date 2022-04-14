package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.TextContent;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class CommentCreation {
  @Getter
  @Setter
  @Valid
  @NotNull
  public String description;
}
