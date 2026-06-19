package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Entity
@Table(name = "notebook")
@JsonPropertyOrder({
  "id",
  "notebookSettings",
  "creatorId",
  "name",
  "circle",
  "description",
  "createdAt",
  "updatedAt"
})
public class Notebook extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creator;

  @OneToOne
  @JoinColumn(name = "ownership_id")
  @Getter
  @Setter
  @JsonIgnore
  private Ownership ownership;

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

  @Column(name = "updated_at")
  @Getter
  @Setter
  @NonNull
  private Timestamp updatedAt;

  @Column(name = "created_at", nullable = false)
  @Getter
  @Setter
  @NonNull
  private Timestamp createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notebook_group_id")
  @JsonIgnore
  @Getter
  @Setter
  private NotebookGroup notebookGroup;

  /** Container-owned index markdown (populated by migration; canonical from 10.15 onward). */
  @Column(name = "index_content", columnDefinition = "mediumtext")
  @Getter
  @Setter
  private String indexContent;

  @Column(name = "description")
  @Getter
  @Setter
  private String description;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public String getName() {
    return name == null ? "" : name;
  }

  public void setName(String name) {
    this.name = name == null ? "" : name;
  }

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

  public String getCreatorId() {
    return creator.getExternalIdentifier();
  }

  public Circle getCircle() {
    return getOwnership().getCircle();
  }
}
