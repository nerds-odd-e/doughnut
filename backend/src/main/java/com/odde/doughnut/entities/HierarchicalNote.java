package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.models.NoteViewer;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Table(name = "hierarchical_note")
@PrimaryKeyJoinColumn(name = "note_id")
public class HierarchicalNote extends Note {
  public HierarchicalNote() {}

}
