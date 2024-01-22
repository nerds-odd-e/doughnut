package com.odde.doughnut.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
