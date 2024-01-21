package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subscription")
@JsonPropertyOrder({"headNote", "title", "shortDescription"})
public class Subscription extends EntityIdentifiedByIdOnly {
  @Column(name = "daily_target_of_new_notes")
  @Getter
  @Setter
  private Integer dailyTargetOfNewNotes = 5;

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @Getter
  @Setter
  private User user;

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Notebook notebook;

  public String getTitle() {
    return notebook.getHeadNote().getTopic();
  }

  public Note getHeadNote() {
    return notebook.getHeadNote();
  }
}
