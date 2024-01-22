package com.odde.doughnut.entities;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notes_closure")
public class NotesClosure extends EntityIdentifiedByIdOnly {
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
