package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_question")
@JsonPropertyOrder({"id", "stem", "options", "correctAnswerIndex", "mainTopic", "imageWithMask"})
public class QuizQuestionEntity extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note note;

  @Column(name = "raw_json_question")
  private String rawJsonQuestion;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "correct_answer_index")
  @Getter
  @Setter
  private Integer correctAnswerIndex;

  @Column(name = "check_spell")
  @Getter
  @Setter
  private Boolean checkSpell;

  @Column(name = "has_image")
  @Getter
  @Setter
  private Boolean hasImage;

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    try {
      return new ObjectMapper().readValue(this.rawJsonQuestion, MCQWithAnswer.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @JsonIgnore
  public void setMcqWithAnswer(MCQWithAnswer mcqWithAnswer) {
    this.rawJsonQuestion = mcqWithAnswer.toJsonString();
    this.correctAnswerIndex = mcqWithAnswer.correctChoiceIndex;
  }

  public boolean checkAnswer(Answer answer) {
    if (checkSpell != null && checkSpell) {
      return getNote().matchAnswer(answer.getSpellingAnswer());
    }
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  public String getStem() {
    return getMcqWithAnswer().stem;
  }

  public ImageWithMask getImageWithMask() {
    if (hasImage != null && hasImage) return getNote().getImageWithMask();
    return null;
  }

  public List<QuizQuestion.Choice> getOptions() {
    MCQWithAnswer mcqWithAnswer = getMcqWithAnswer();
    if (mcqWithAnswer.choices == null) {
      return List.of();
    }
    return mcqWithAnswer.choices.stream()
        .map(
            choice -> {
              QuizQuestion.Choice option = new QuizQuestion.Choice();
              option.setDisplay(choice);
              return option;
            })
        .toList();
  }

  public QuizQuestion getQuizQuestion() {
    return new QuizQuestion(
        getId(),
        getStem(),
        getNote().getNotebook().getHeadNote(),
        getOptions(),
        getImageWithMask());
  }
}
