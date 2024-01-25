package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    return notebook.getHeadNote().getTopicConstructor();
  }

  public Note getHeadNote() {
    return notebook.getHeadNote();
  }
}
