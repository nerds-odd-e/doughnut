package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.donut.algorithms.ClozedString;
import com.odde.donut.algorithms.FrontmatterAliases;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.NoteLeadingFrontmatter;
import com.odde.donut.algorithms.NoteTitle;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.NoteTopology;
import com.odde.donut.entities.converters.DisplayNameConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "note")
@JsonPropertyOrder({"noteTopology", "content"})
public class Note extends EntityIdentifiedByIdOnly {
  public static final int MAX_TITLE_LENGTH = 150;

  public static final String NOTE_OF_CURRENT_FOCUS = "note of current focus";

  @ManyToOne
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  private Notebook notebook;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "folder_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Folder folder;

  @Column(name = "content", columnDefinition = "mediumtext")
  @Getter
  @Setter
  @JsonPropertyDescription("The note content is in markdown format.")
  private String content;

  @Column(name = "title", nullable = false)
  @Convert(converter = DisplayNameConverter.class)
  @JsonIgnore
  private DisplayName title = new DisplayName("");

  @NotBlank
  @Size(max = MAX_TITLE_LENGTH)
  public String getTitle() {
    return title.value();
  }

  public void setTitle(DisplayName title) {
    this.title = title;
  }

  @Column(name = "created_at")
  @Setter
  @Getter
  @NotNull
  @JsonIgnore
  private Timestamp createdAt;

  @Setter
  @Column(name = "deleted_at")
  @Getter
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Timestamp deletedAt;

  @OneToMany(mappedBy = "note")
  @JsonIgnore
  private Set<MemoryTracker> memoryTrackers;

  @Column(name = "updated_at")
  @Getter
  @Setter
  @NotNull
  @JsonIgnore
  private Timestamp updatedAt;

  @OneToMany(mappedBy = "note")
  @Getter
  @JsonIgnore
  private List<Mcq> mcqs = new ArrayList<>();

  public static <T extends Note> List<T> filterDeletedUnmodifiableNoteList(List<T> notes) {
    return notes.stream().filter(n -> n.getDeletedAt() == null).toList();
  }

  @JsonIgnore
  public NoteTitle getNoteTitle() {
    return new NoteTitle(getTitle());
  }

  @JsonIgnore
  public ClozedString createMaskedContentForRecall() {
    if (isBodyContentBlank()) return new ClozedString(null, "");

    return ClozedString.forMarkdownWithMarkMasks(
            NoteContentMarkdown.bodyWithoutLeadingFrontmatter(getContent()))
        .hide(getNoteTitle())
        .hideAliases(FrontmatterAliases.fromNoteContent(getContent()));
  }

  @JsonIgnore
  public boolean isBodyContentBlank() {
    return NoteContentMarkdown.isBodyContentBlank(getContent());
  }

  @Override
  public String toString() {
    return "Note{" + "id=" + id + ", title='" + getTitle() + '\'' + '}';
  }

  @JsonIgnore
  public void assignNotebook(Notebook notebook) {
    setNotebook(notebook);
  }

  private void setNotebook(Notebook notebook) {
    this.notebook = notebook;
  }

  public void prependContent(String addition) {
    setContent(NoteLeadingFrontmatter.prependToBody(getContent(), addition));
  }

  @JsonIgnore
  public boolean matchAnswer(String spellingAnswer) {
    if (getNoteTitle().matchesForRecall(spellingAnswer)) {
      return true;
    }
    return FrontmatterAliases.matchesFromNoteContent(getContent(), spellingAnswer);
  }

  @NonNull
  public NoteTopology getNoteTopology() {
    NoteTopology noteTopology = new NoteTopology();
    noteTopology.setId(getId());
    noteTopology.setTitle(getTitle());
    Objects.requireNonNull(getNotebook());
    noteTopology.setCreatedAt(getCreatedAt());
    noteTopology.setUpdatedAt(getUpdatedAt());
    return noteTopology;
  }

  @JsonIgnore
  public String getNoteDescription() {
    Map<String, Object> shape = new LinkedHashMap<>();
    shape.put("notebook", getNotebook() != null ? getNotebook().getName() : null);
    shape.put("title", getTitle());
    shape.put("content", getContent());
    String prettyString =
        new ObjectMapperConfig().objectMapper().valueToTree(shape).toPrettyString();
    return """
        The %s (in JSON format):
        %s
        """
        .formatted(NOTE_OF_CURRENT_FOCUS, prettyString);
  }

  public void initializeNewNote(
      Notebook notebookOrNull, Timestamp currentUTCTimestamp, String title) {
    setNotebook(notebookOrNull);
    setUpdatedAt(currentUTCTimestamp);
    setTitle(new DisplayName(title));
    setCreatedAt(currentUTCTimestamp);
  }
}
