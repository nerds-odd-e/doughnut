package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Thing;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class LinkViewed {
  @Getter
  @Setter
  @NotNull
  @JsonIgnoreProperties("sourceNote")
  private List<Thing> direct;

  @Getter
  @Setter
  @NotNull
  @JsonIgnoreProperties("targetNote")
  private List<Thing> reverse;

  public boolean notEmpty() {
    return (direct != null && !direct.isEmpty()) || (reverse != null && !reverse.isEmpty());
  }
}
