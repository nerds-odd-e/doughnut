package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.controllers.dto.QuizQuestionInNotebook;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "quiz_question")
@JsonPropertyOrder({"id", "multipleChoicesQuestion", "headNote", "imageWithMask"})
public class QuizQuestion extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  private Note note;

  @Embedded @JsonIgnore private QuizQuestion1 quizQuestion1 = new QuizQuestion1();

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    return quizQuestion1.getMcqWithAnswer();
  }

  @JsonIgnore
  public boolean checkAnswer(Answer answer) {
    if (quizQuestion1.getCheckSpell() != null && quizQuestion1.getCheckSpell()) {
      return getNote().matchAnswer(answer.getSpellingAnswer());
    }
    return Objects.equals(answer.getChoiceIndex(), quizQuestion1.getCorrectAnswerIndex());
  }

  public ImageWithMask getImageWithMask() {
    return quizQuestion1.getImageWithMask();
  }

  public static QuizQuestion fromMCQWithAnswer(MCQWithAnswer MCQWithAnswer, Note note) {
    QuizQuestion quizQuestionAIQuestion = new QuizQuestion();
    quizQuestionAIQuestion.setNote(note);
    quizQuestionAIQuestion
        .getQuizQuestion1()
        .setMultipleChoicesQuestion(MCQWithAnswer.getMultipleChoicesQuestion());
    quizQuestionAIQuestion
        .getQuizQuestion1()
        .setCorrectAnswerIndex(MCQWithAnswer.getCorrectChoiceIndex());
    return quizQuestionAIQuestion;
  }

  @JsonIgnore
  public void setCorrectAnswerIndex(int i) {
    getQuizQuestion1().setCorrectAnswerIndex(i);
  }

  public void setMultipleChoicesQuestion(MultipleChoicesQuestion mcq) {
    getQuizQuestion1().setMultipleChoicesQuestion(mcq);
  }

  @NotNull
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    return getQuizQuestion1().getMultipleChoicesQuestion();
  }

  @JsonIgnore
  public void setApproved(boolean approved) {
    getQuizQuestion1().setApproved(approved);
  }

  @JsonIgnore
  public void setNote(Note value) {
    this.note = value;
  }

  @JsonIgnore
  public Integer getCorrectAnswerIndex() {
    return getQuizQuestion1().getCorrectAnswerIndex();
  }

  @JsonIgnore
  public void setCheckSpell(boolean b) {
    getQuizQuestion1().setCheckSpell(b);
  }

  @JsonIgnore
  public boolean isApproved() {
    return getQuizQuestion1().isApproved();
  }

  public QuizQuestionInNotebook toQuizQuestionInNotebook() {
    QuizQuestionInNotebook quizQuestionInNotebook = new QuizQuestionInNotebook();
    quizQuestionInNotebook.setNotebook(getNote().getNotebook());
    quizQuestionInNotebook.setMultipleChoicesQuestion(getMultipleChoicesQuestion());
    quizQuestionInNotebook.setId(getId());
    quizQuestionInNotebook.setImageWithMask(getImageWithMask());
    return quizQuestionInNotebook;
  }
}
