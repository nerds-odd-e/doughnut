package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.converters.MCQToJsonConverter;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
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

  @Column(name = "raw_json_question")
  @Convert(converter = MCQToJsonConverter.class)
  @NotNull
  private MultipleChoicesQuestion multipleChoicesQuestion;

  @Column(name = "created_at")
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "correct_answer_index")
  private Integer correctAnswerIndex;

  @Column(name = "is_approved")
  private boolean approved;

  @JsonIgnore
  @Column(name = "is_contested")
  private boolean contested;

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
    mcqWithAnswer.setF0__multipleChoicesQuestion(getMultipleChoicesQuestion());
    mcqWithAnswer.setF1__correctChoiceIndex(correctAnswerIndex == null ? -1 : correctAnswerIndex);
    return mcqWithAnswer;
  }

  @JsonIgnore
  public boolean checkAnswer(AnswerDTO answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  public static PredefinedQuestion fromMCQWithAnswer(MCQWithAnswer MCQWithAnswer, Note note) {
    PredefinedQuestion predefinedQuestion = new PredefinedQuestion();
    predefinedQuestion.setNote(note);
    predefinedQuestion.setMultipleChoicesQuestion(MCQWithAnswer.getF0__multipleChoicesQuestion());
    predefinedQuestion.setCorrectAnswerIndex(MCQWithAnswer.getF1__correctChoiceIndex());
    return predefinedQuestion;
  }

  @Override
  public String toString() {
    return "PredefinedQuestion{" + "id=" + id + '}';
  }
}
