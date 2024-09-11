package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.QuizQuestionInNotebook;
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
@Table(name = "question_and_answer")
public class PredefinedQuestion extends EntityIdentifiedByIdOnly {
  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  private Note note;

  @OneToOne(mappedBy = "predefinedQuestion", cascade = CascadeType.ALL)
  @NotNull
  private QuizQuestion quizQuestion = new QuizQuestion();

  @Column(name = "raw_json_question")
  @Convert(converter = MCQToJsonConverter.class)
  @NotNull
  private MultipleChoicesQuestion multipleChoicesQuestion;

  @Column(name = "check_spell")
  private Boolean checkSpell;

  @Embedded ImageWithMask imageWithMask;

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
    mcqWithAnswer.setMultipleChoicesQuestion(getMultipleChoicesQuestion());
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

  public static PredefinedQuestion fromMCQWithAnswer(MCQWithAnswer MCQWithAnswer, Note note) {
    PredefinedQuestion quizQuestionAIPredefinedQuestion = new PredefinedQuestion();
    quizQuestionAIPredefinedQuestion.setNote(note);
    quizQuestionAIPredefinedQuestion.setMultipleChoicesQuestion(
        MCQWithAnswer.getMultipleChoicesQuestion());
    quizQuestionAIPredefinedQuestion.setCorrectAnswerIndex(MCQWithAnswer.getCorrectChoiceIndex());
    // for in memory consistency
    quizQuestionAIPredefinedQuestion
        .getQuizQuestion()
        .setPredefinedQuestion(quizQuestionAIPredefinedQuestion);
    return quizQuestionAIPredefinedQuestion;
  }

  public QuizQuestionInNotebook toQuizQuestionInNotebook() {
    QuizQuestionInNotebook quizQuestionInNotebook = new QuizQuestionInNotebook();
    quizQuestionInNotebook.setNotebook(getNote().getNotebook());
    quizQuestionInNotebook.setQuizQuestion(getQuizQuestion());
    return quizQuestionInNotebook;
  }

  @Override
  public String toString() {
    // Fixing StackoverflowError when calling toString on PredefinedQuestion or QuizQuestion
    return "PredefinedQuestion{" + "id=" + id + '}';
  }
}
