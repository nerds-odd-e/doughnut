package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_question")
@JsonPropertyOrder({"id", "stem", "headNote", "choices", "imageWithMask"})
public class QuizQuestion extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  @JsonIgnore
  private Note note;

  @Column(name = "raw_json_question")
  @JsonIgnore
  private String rawJsonQuestion;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "correct_answer_index")
  @Getter
  @JsonIgnore
  private Integer correctAnswerIndex;

  @Column(name = "check_spell")
  @Getter
  @Setter
  @JsonIgnore
  private Boolean checkSpell;

  @Column(name = "has_image")
  @Getter
  @Setter
  @JsonIgnore
  private Boolean hasImage;

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    MultipleChoicesQuestion mcq = getMultipleChoicesQuestion();
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
    mcq.populate(mcqWithAnswer);
    mcqWithAnswer.correctChoiceIndex = correctAnswerIndex;
    return mcqWithAnswer;
  }

  @NotNull
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    try {
      return new ObjectMapper().readValue(this.rawJsonQuestion, MultipleChoicesQuestion.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @JsonIgnore
  public void setMcqWithAnswer(MCQWithAnswer mcqWithAnswer) {
    this.rawJsonQuestion = mcqWithAnswer.cloneQuestion().toJsonString();
    this.correctAnswerIndex = mcqWithAnswer.correctChoiceIndex;
  }

  @JsonIgnore
  public boolean checkAnswer(Answer answer) {
    if (checkSpell != null && checkSpell) {
      return getNote().matchAnswer(answer.getSpellingAnswer());
    }
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  public ImageWithMask getImageWithMask() {
    if (hasImage != null && hasImage) return getNote().getImageWithMask();
    return null;
  }

  @NotNull
  public Note getHeadNote() {
    return getNote().getNotebook().getHeadNote();
  }

  @Data
  public static class Choice {
    private String display;
  }
}
