package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "thing")
@JsonPropertyOrder({"note", "link", "linkType", "sourceNote", "targetNote", "createdAt"})
public class Thing extends EntityIdentifiedByIdOnly {
  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  @Nullable
  private Note note;

  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creator;

  @JsonIgnore
  public Note getParentNote() {
    return getNote().getParent();
  }

  public void setSourceNote(Note from) {
    getNote().setParentNote(from);
  }

  @Nullable
  public Note getSourceNote() {
    return getParentNote();
  }

  @Nullable
  public Note getTargetNote() {
    return getNote().getTargetNote();
  }

  public void setTargetNote(Note to) {
    getNote().setTargetNote(to);
  }

  @Nullable
  public LinkType getLinkType() {
    return getNote().getLinkType();
  }

  public void setLinkType(LinkType linkType) {
    getNote().setLinkType(linkType);
  }
}
