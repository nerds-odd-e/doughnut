package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.donut.controllers.dto.AnswerDTO;
import com.odde.donut.entities.converters.MCQToJsonConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mcq")
@JsonPropertyOrder({
  "id",
  "correctAnswerIndex",
  "contextSeed",
  "testedFocus",
  "validationRationale",
  "questionStem",
  "responseChoices"
})
public class Mcq extends EntityIdentifiedByIdOnly {
  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  private Note note;

  @JsonIgnore
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
  @Schema(hidden = true)
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    return multipleChoicesQuestion;
  }

  @JsonIgnore
  public void setMultipleChoicesQuestion(MultipleChoicesQuestion multipleChoicesQuestion) {
    this.multipleChoicesQuestion = multipleChoicesQuestion;
  }

  @JsonProperty(required = true)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public String getQuestionStem() {
    return multipleChoicesQuestion == null ? null : multipleChoicesQuestion.getQuestionStem();
  }

  public void setQuestionStem(String questionStem) {
    stemAndChoices().setQuestionStem(questionStem);
  }

  @JsonProperty(required = true)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public List<String> getResponseChoices() {
    return multipleChoicesQuestion == null ? null : multipleChoicesQuestion.getResponseChoices();
  }

  public void setResponseChoices(List<String> responseChoices) {
    stemAndChoices().setResponseChoices(responseChoices);
  }

  public Mcq withoutSolution() {
    Mcq copy = new Mcq();
    copy.id = this.id;
    if (multipleChoicesQuestion == null) {
      return copy;
    }
    copy.setQuestionStem(getQuestionStem());
    List<String> choices = getResponseChoices();
    if (choices != null) {
      copy.setResponseChoices(List.copyOf(choices));
    }
    return copy;
  }

  @JsonIgnore
  public boolean checkAnswer(AnswerDTO answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  @Override
  public String toString() {
    return "Mcq{" + "id=" + id + '}';
  }

  private MultipleChoicesQuestion stemAndChoices() {
    if (multipleChoicesQuestion == null) {
      multipleChoicesQuestion = new MultipleChoicesQuestion();
    }
    return multipleChoicesQuestion;
  }
}
