package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subscription")
@JsonPropertyOrder({"headNote", "title", "shortDescription"})
public class Subscription {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "daily_target_of_new_notes")
  @Getter
  @Setter
  private Integer dailyTargetOfNewNotes = 5;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @Getter
  @Setter
  private User user;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Notebook notebook;

  public String getTitle() {
    return notebook.getHeadNote().getTitle();
  }

  public String getShortDescription() {
    return notebook.getHeadNote().getShortDescription();
  }

  public Note getHeadNote() {
    return notebook.getHeadNote();
  }
}
