package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Entity
@Table(name = "notebook")
@JsonPropertyOrder({"id", "headNote"})
public class Notebook extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creatorEntity;

  @OneToOne
  @JoinColumn(name = "ownership_id")
  @Getter
  @Setter
  @JsonIgnore
  private Ownership ownership;

  @JoinTable(
      name = "notebook_head_note",
      joinColumns = {@JoinColumn(name = "notebook_id", referencedColumnName = "id")},
      inverseJoinColumns = {@JoinColumn(name = "head_note_id", referencedColumnName = "id")})
  @OneToOne
  @Getter
  @Setter
  @NonNull
  private Note headNote;

  @OneToMany(mappedBy = "notebook", cascade = CascadeType.DETACH)
  @JsonIgnore
  private final List<Note> notes = new ArrayList<>();

  @Embedded @Getter @NonNull private NotebookSettings notebookSettings = new NotebookSettings();

  @Column(name = "deleted_at")
  @Setter
  @JsonIgnore
  private Timestamp deletedAt;

  @OneToMany(mappedBy = "notebook", cascade = CascadeType.DETACH)
  @JsonIgnore
  private List<Subscription> subscriptions;

  @JsonIgnore
  public List<Note> getNotes() {
    return Note.filterDeletedUnmodifiableNoteList(notes);
  }

  // Hibernate and JPA does not maintain the consistency of the bidirectional relationships
  // Here we add the note to the notes of notebook in memory to avoid reload the notebook from
  // database
  public void addNoteInMemoryToSupportUnitTestOnly(Note note) {
    this.notes.add(note);
  }
}
