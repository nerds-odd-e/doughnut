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
@Table(name = "mcq")
public class Mcq extends EntityIdentifiedByIdOnly {
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

  @JsonIgnore
  @Column(name = "is_contested")
  private boolean contested;

  @Column(name = "context_seed")
  private Long contextSeed;

  @Column(name = "tested_focus", columnDefinition = "TEXT")
  private String testedFocus;

  @Column(name = "validation_rationale", columnDefinition = "TEXT")
  private String validationRationale;

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
    mcqWithAnswer.setQuestion(getMultipleChoicesQuestion());
    mcqWithAnswer.setSolutionChoiceIndex(correctAnswerIndex == null ? -1 : correctAnswerIndex);
    mcqWithAnswer.setTestedFocus(testedFocus);
    mcqWithAnswer.setValidationRationale(validationRationale);
    return mcqWithAnswer;
  }

  @JsonIgnore
  public boolean checkAnswer(AnswerDTO answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  public static Mcq fromMCQWithAnswer(MCQWithAnswer MCQWithAnswer, Note note) {
    return fromMCQWithAnswer(MCQWithAnswer, note, null);
  }

  public static Mcq fromMCQWithAnswer(MCQWithAnswer MCQWithAnswer, Note note, Long contextSeed) {
    Mcq mcq = new Mcq();
    mcq.setNote(note);
    mcq.setMultipleChoicesQuestion(MCQWithAnswer.getQuestion());
    mcq.setCorrectAnswerIndex(MCQWithAnswer.getSolutionChoiceIndex());
    mcq.setContextSeed(contextSeed);
    mcq.setTestedFocus(MCQWithAnswer.getTestedFocus());
    mcq.setValidationRationale(MCQWithAnswer.getValidationRationale());
    return mcq;
  }

  @Override
  public String toString() {
    return "Mcq{" + "id=" + id + '}';
  }
}
