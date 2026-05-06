package com.odde.doughnut.entities;

import static com.odde.doughnut.controllers.dto.ApiError.ErrorType.ASSESSMENT_SERVICE_ERROR;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.utils.Randomizer;
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
  "certifiable",
  "notebookSettings",
  "creatorId",
  "name",
  "circle",
  "description"
})
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

  @OneToOne(mappedBy = "notebook")
  @Getter
  @JsonIgnore
  private NotebookCertificateApproval notebookCertificateApproval;

  @Column(name = "updated_at")
  @Getter
  @Setter
  @NonNull
  private Timestamp updated_at;

  @Column(name = "created_at", nullable = false)
  @Getter
  @Setter
  @JsonIgnore
  @NonNull
  private Timestamp createdAt;

  @OneToOne(mappedBy = "notebook", fetch = FetchType.LAZY)
  @JsonIgnore
  @Getter
  private NotebookAiAssistant notebookAiAssistant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notebook_group_id")
  @JsonIgnore
  @Getter
  @Setter
  private NotebookGroup notebookGroup;

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

  public boolean isCertifiable() {
    return notebookCertificateApproval != null
        && notebookCertificateApproval.getLastApprovalTime() != null;
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
    return creatorEntity.getExternalIdentifier();
  }

  @JsonIgnore
  public List<PredefinedQuestion> getApprovedPredefinedQuestionsForAssessment(
      Randomizer randomizer) {
    Integer numberOfQuestion = getNotebookSettings().getNumberOfQuestionsInAssessment();
    if (numberOfQuestion == null || numberOfQuestion == 0) {
      throw new ApiException(
          "The assessment is not available",
          ASSESSMENT_SERVICE_ERROR,
          "The assessment is not available");
    }

    List<PredefinedQuestion> questions =
        randomizer.shuffle(getNotes()).stream()
            .flatMap(
                note ->
                    randomizer
                        .chooseOneRandomly(
                            note.getPredefinedQuestions().stream()
                                .filter(PredefinedQuestion::isApproved)
                                .toList())
                        .stream())
            .limit(numberOfQuestion)
            .toList();

    if (questions.size() < numberOfQuestion) {
      throw new ApiException(
          "Not enough questions", ASSESSMENT_SERVICE_ERROR, "Not enough questions");
    }
    return questions;
  }

  public Circle getCircle() {
    return getOwnership().getCircle();
  }
}
