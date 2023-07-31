package com.odde.doughnut.entities;

import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class TextContent {

  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  @Getter
  @Setter
  private String title = "";

  @Getter @Setter private String description;
}
