package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Thing;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class LinkViewed {
  @Getter
  @Setter
  @JsonIgnoreProperties("sourceNote")
  private List<Thing> direct;

  @Getter
  @Setter
  @JsonIgnoreProperties("targetNote")
  private List<Thing> reverse;

  public boolean notEmpty() {
    return (direct != null && !direct.isEmpty()) || (reverse != null && !reverse.isEmpty());
  }
}
