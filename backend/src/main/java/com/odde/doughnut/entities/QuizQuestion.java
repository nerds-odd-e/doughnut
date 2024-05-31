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
import java.util.List;
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
    try {
      MultipleChoicesQuestion mcq =
          new ObjectMapper().readValue(this.rawJsonQuestion, MultipleChoicesQuestion.class);
      MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
      mcq.populate(mcqWithAnswer);
      return mcqWithAnswer;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @JsonIgnore
  private MultipleChoicesQuestion getMultipleChoicesQuestion() {
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

  public String getStem() {
    return getMultipleChoicesQuestion().stem;
  }

  public ImageWithMask getImageWithMask() {
    if (hasImage != null && hasImage) return getNote().getImageWithMask();
    return null;
  }

  public List<Choice> getChoices() {
    MultipleChoicesQuestion mcq = getMultipleChoicesQuestion();
    if (mcq.choices == null) {
      return List.of();
    }
    return mcq.choices.stream()
        .map(
            choice -> {
              Choice option = new Choice();
              option.setDisplay(choice);
              return option;
            })
        .toList();
  }

  @NotNull
  public Note getHeadNote() {
    return getNote().getNotebook().getHeadNote();
  }

  @Data
  public static class Choice {
    private boolean isImage = false;
    private String display;
    private ImageWithMask imageWithMask;
  }
}
