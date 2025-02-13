package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "predefined_question")
public class PredefinedQuestion extends EntityIdentifiedByIdOnly {
  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  private Note note;

  @Embedded @NotNull private BareQuestion bareQuestion = new BareQuestion();

  @Column(name = "created_at")
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "correct_answer_index")
  private Integer correctAnswerIndex;

  @Column(name = "is_approved")
  private boolean approved;

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
    mcqWithAnswer.setMultipleChoicesQuestion(bareQuestion.getMultipleChoicesQuestion());
    mcqWithAnswer.setCorrectChoiceIndex(correctAnswerIndex == null ? -1 : correctAnswerIndex);
    return mcqWithAnswer;
  }

  @JsonIgnore
  public boolean checkAnswer(AnswerDTO answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  public static PredefinedQuestion fromMCQWithAnswer(MCQWithAnswer MCQWithAnswer, Note note) {
    PredefinedQuestion predefinedQuestion = new PredefinedQuestion();
    predefinedQuestion.setNote(note);
    predefinedQuestion.bareQuestion.setMultipleChoicesQuestion(
        MCQWithAnswer.getMultipleChoicesQuestion());
    predefinedQuestion.setCorrectAnswerIndex(MCQWithAnswer.getCorrectChoiceIndex());
    return predefinedQuestion;
  }

  @Override
  public String toString() {
    return "PredefinedQuestion{" + "id=" + id + '}';
  }
}
