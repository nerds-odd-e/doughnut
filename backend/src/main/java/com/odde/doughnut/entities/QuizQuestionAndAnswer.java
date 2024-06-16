package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.QuizQuestionInNotebook;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "quiz_question_and_answer")
public class QuizQuestionAndAnswer extends EntityIdentifiedByIdOnly {
  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  private Note note;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "quiz_question_id", referencedColumnName = "id")
  @NotNull
  private QuizQuestion quizQuestion = new QuizQuestion();

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
    // for in memory consistency
    quizQuestionAIQuestionAndAnswer
        .getQuizQuestion()
        .setQuizQuestionAndAnswer(quizQuestionAIQuestionAndAnswer);
    return quizQuestionAIQuestionAndAnswer;
  }

  public QuizQuestionInNotebook toQuizQuestionInNotebook() {
    QuizQuestionInNotebook quizQuestionInNotebook = new QuizQuestionInNotebook();
    quizQuestionInNotebook.setNotebook(getNote().getNotebook());
    quizQuestionInNotebook.setQuizQuestion(getQuizQuestion());
    return quizQuestionInNotebook;
  }
}
