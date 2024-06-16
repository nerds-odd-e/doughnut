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
public class QuizQuestionAndAnswer extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  private Note note;

  @Embedded @NotNull private QuizQuestion quizQuestion = new QuizQuestion();

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    return quizQuestion.getMcqWithAnswer();
  }

  @JsonIgnore
  public boolean checkAnswer(Answer answer) {
    if (quizQuestion.getCheckSpell() != null && quizQuestion.getCheckSpell()) {
      return getNote().matchAnswer(answer.getSpellingAnswer());
    }
    return Objects.equals(answer.getChoiceIndex(), quizQuestion.getCorrectAnswerIndex());
  }

  public ImageWithMask getImageWithMask() {
    return quizQuestion.getImageWithMask();
  }

  public static QuizQuestionAndAnswer fromMCQWithAnswer(MCQWithAnswer MCQWithAnswer, Note note) {
    QuizQuestionAndAnswer quizQuestionAIQuestionAndAnswer = new QuizQuestionAndAnswer();
    quizQuestionAIQuestionAndAnswer.setNote(note);
    quizQuestionAIQuestionAndAnswer
        .getQuizQuestion()
        .setMultipleChoicesQuestion(MCQWithAnswer.getMultipleChoicesQuestion());
    quizQuestionAIQuestionAndAnswer
        .getQuizQuestion()
        .setCorrectAnswerIndex(MCQWithAnswer.getCorrectChoiceIndex());
    return quizQuestionAIQuestionAndAnswer;
  }

  @JsonIgnore
  public void setCorrectAnswerIndex(int i) {
    getQuizQuestion().setCorrectAnswerIndex(i);
  }

  public void setMultipleChoicesQuestion(MultipleChoicesQuestion mcq) {
    getQuizQuestion().setMultipleChoicesQuestion(mcq);
  }

  @NotNull
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    return getQuizQuestion().getMultipleChoicesQuestion();
  }

  @JsonIgnore
  public void setApproved(boolean approved) {
    getQuizQuestion().setApproved(approved);
  }

  @JsonIgnore
  public void setNote(Note value) {
    this.note = value;
  }

  @JsonIgnore
  public Integer getCorrectAnswerIndex() {
    return getQuizQuestion().getCorrectAnswerIndex();
  }

  @JsonIgnore
  public void setCheckSpell(boolean b) {
    getQuizQuestion().setCheckSpell(b);
  }

  @JsonIgnore
  public boolean isApproved() {
    return getQuizQuestion().isApproved();
  }

  public QuizQuestionInNotebook toQuizQuestionInNotebook() {
    QuizQuestionInNotebook quizQuestionInNotebook = new QuizQuestionInNotebook();
    quizQuestionInNotebook.setNotebook(getNote().getNotebook());
    // make sure the id is the same as the quiz question id
    getQuizQuestion().setId(getId());
    quizQuestionInNotebook.setQuizQuestion(getQuizQuestion());
    return quizQuestionInNotebook;
  }
}
