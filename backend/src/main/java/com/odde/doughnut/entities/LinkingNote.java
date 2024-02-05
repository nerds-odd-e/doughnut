package com.odde.doughnut.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "linking_note")
@PrimaryKeyJoinColumn(name = "note_id")
public class LinkingNote extends Note {
  private LinkingNote() {}
}
