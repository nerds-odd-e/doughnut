package com.odde.doughnut.entities;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
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

  @JsonIgnore
  public String getNotebookDump() {
    List<Note.NoteBrief> noteBriefs = getNoteBriefs();
    return defaultObjectMapper().valueToTree(noteBriefs).toPrettyString();
  }

  @JsonIgnore
  public List<Note.NoteBrief> getNoteBriefs() {
    List<Note.NoteBrief> noteBriefs =
        notes.stream()
            .sorted(
                Comparator.comparing(Note::getParentId, Comparator.nullsFirst(Integer::compare))
                    .thenComparing(Note::getSiblingOrder, Comparator.nullsFirst(Long::compare)))
            .map(Note::getNoteBrief)
            .toList();
    ;
    return noteBriefs;
  }

  public String getCreatorId() {
    return creatorEntity.getExternalIdentifier();
  }

  public String getCertifiedBy() {
    return "Terry";
  }
}
