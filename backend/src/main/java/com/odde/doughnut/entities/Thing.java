package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({"note", "link", "linkType", "sourceNote", "targetNote", "createdAt"})
public class Thing {
  @Getter @Setter private Note note;

  public Note getSourceNote() {
    return getNote().getParent();
  }

  public Note getTargetNote() {
    return getNote().getTargetNote();
  }
}
