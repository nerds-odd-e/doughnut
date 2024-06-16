package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.controllers.dto.QuizQuestionInNotebook;
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
@Table(name = "quiz_question")
public class QuizQuestionAndAnswer extends EntityIdentifiedByIdOnly {
  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  private Note note;

  @Embedded @NotNull private QuizQuestion quizQuestion = new QuizQuestion();

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
    mcqWithAnswer.setMultipleChoicesQuestion(quizQuestion.getMultipleChoicesQuestion());
    mcqWithAnswer.setCorrectChoiceIndex(correctAnswerIndex == null ? -1 : correctAnswerIndex);
    mcqWithAnswer.setApproved(approved);
    mcqWithAnswer.setId(id);
    return mcqWithAnswer;
  }

  @JsonIgnore
  public boolean checkAnswer(Answer answer) {
    if (quizQuestion.getCheckSpell() != null && quizQuestion.getCheckSpell()) {
      return getNote().matchAnswer(answer.getSpellingAnswer());
    }
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  public static QuizQuestionAndAnswer fromMCQWithAnswer(MCQWithAnswer MCQWithAnswer, Note note) {
    QuizQuestionAndAnswer quizQuestionAIQuestionAndAnswer = new QuizQuestionAndAnswer();
    quizQuestionAIQuestionAndAnswer.setNote(note);
    quizQuestionAIQuestionAndAnswer
        .getQuizQuestion()
        .setMultipleChoicesQuestion(MCQWithAnswer.getMultipleChoicesQuestion());
    quizQuestionAIQuestionAndAnswer.setCorrectAnswerIndex(MCQWithAnswer.getCorrectChoiceIndex());
    return quizQuestionAIQuestionAndAnswer;
  }

  public void setMultipleChoicesQuestion(MultipleChoicesQuestion mcq) {
    getQuizQuestion().setMultipleChoicesQuestion(mcq);
  }

  @NotNull
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    return getQuizQuestion().getMultipleChoicesQuestion();
  }

  @JsonIgnore
  public void setCheckSpell(boolean b) {
    getQuizQuestion().setCheckSpell(b);
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
