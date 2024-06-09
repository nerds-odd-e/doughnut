package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.converters.MCQToJsonConverter;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Objects;
import lombok.Data;

@Entity
@Data
@Table(name = "quiz_question")
@JsonPropertyOrder({"id", "multipleChoicesQuestion", "headNote", "imageWithMask"})
public class QuizQuestion extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  private Note note;

  @Column(name = "raw_json_question")
  @Convert(converter = MCQToJsonConverter.class)
  @NotNull
  private MultipleChoicesQuestion multipleChoicesQuestion;

  @Column(name = "created_at")
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "correct_answer_index")
  @JsonIgnore
  private Integer correctAnswerIndex;

  @Column(name = "check_spell")
  @JsonIgnore
  private Boolean checkSpell;

  @Column(name = "has_image")
  @JsonIgnore
  private Boolean hasImage;

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
    mcqWithAnswer.setMultipleChoicesQuestion(getMultipleChoicesQuestion());
    mcqWithAnswer.setCorrectChoiceIndex(correctAnswerIndex == null ? -1 : correctAnswerIndex);
    return mcqWithAnswer;
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

  public static QuizQuestion fromMCQWithAnswer(MCQWithAnswer MCQWithAnswer, Note note) {
    QuizQuestion quizQuestionAIQuestion = new QuizQuestion();
    quizQuestionAIQuestion.setNote(note);
    quizQuestionAIQuestion.setMultipleChoicesQuestion(MCQWithAnswer.getMultipleChoicesQuestion());
    quizQuestionAIQuestion.setCorrectAnswerIndex(MCQWithAnswer.getCorrectChoiceIndex());
    return quizQuestionAIQuestion;
  }
}
