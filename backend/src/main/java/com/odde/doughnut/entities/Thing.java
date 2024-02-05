package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@JsonPropertyOrder({"note", "link", "linkType", "sourceNote", "targetNote", "createdAt"})
public class Thing {
  @Getter @Setter @Nullable private Note note;

  @Nullable
  public Integer getId() {
    return getNote().getId();
  }

  @Nullable
  public Note getSourceNote() {
    return getNote().getParent();
  }

  @Nullable
  public Note getTargetNote() {
    return getNote().getTargetNote();
  }

  @Nullable
  public LinkType getLinkType() {
    return getNote().getLinkType();
  }
}
