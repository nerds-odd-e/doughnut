package com.odde.doughnut.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notes_closure")
public class NotesClosure {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note note;

  @ManyToOne
  @JoinColumn(name = "ancestor_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note ancestor;

  @Getter @Setter private Integer depth = 0;
}
