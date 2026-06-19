package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.NoteTitle;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.NoteTopology;
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

  @NotBlank
  @Size(max = MAX_TITLE_LENGTH)
  @Getter
  @Setter
  @Column(name = "title", nullable = false)
  @JsonIgnore
  private String title = "";

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
  private List<PredefinedQuestion> predefinedQuestions = new ArrayList<>();

  @Embedded @JsonIgnore @Getter private NoteRecallSetting recallSetting = new NoteRecallSetting();

  public static <T extends Note> List<T> filterDeletedUnmodifiableNoteList(List<T> notes) {
    return notes.stream().filter(n -> n.getDeletedAt() == null).toList();
  }

  @JsonIgnore
  public NoteTitle getNoteTitle() {
    return new NoteTitle(getTitle() != null ? getTitle() : "");
  }

  @JsonIgnore
  public ClozedString createMaskedContentForRecall() {
    if (isBodyContentBlank()) return new ClozedString(null, "");

    return ClozedString.forMarkdownWithMarkMasks(
            NoteContentMarkdown.bodyWithoutLeadingFrontmatter(getContent()))
        .hide(getNoteTitle());
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
    String prev = getContent() != null ? getContent() : "";
    String merged = prev.isEmpty() ? addition : addition + "\n\n" + prev;
    setContent(merged);
  }

  @JsonIgnore
  public boolean matchAnswer(String spellingAnswer) {
    return getNoteTitle().matches(spellingAnswer);
  }

  @NonNull
  public NoteTopology getNoteTopology() {
    NoteTopology noteTopology = new NoteTopology();
    noteTopology.setId(getId());
    noteTopology.setTitle(getTitle() != null ? getTitle() : "");
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
    setTitle(title != null ? title : "");
    setCreatedAt(currentUTCTimestamp);
  }

  @JsonIgnore
  public String getNotebookAssistantInstructions() {
    NotebookAiAssistant notebookAiAssistant = getNotebook().getNotebookAiAssistant();
    if (notebookAiAssistant == null) {
      return null;
    }
    return notebookAiAssistant.getAdditionalInstructionsToAi();
  }
}
