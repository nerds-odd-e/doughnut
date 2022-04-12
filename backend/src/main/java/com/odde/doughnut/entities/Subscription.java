package com.odde.doughnut.entities;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subscription")
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
