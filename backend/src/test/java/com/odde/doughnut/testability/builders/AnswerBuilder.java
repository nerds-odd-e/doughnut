package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerViewedByUser;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AnswerBuilder extends EntityBuilder<AnswerViewedByUser> {
  Answer answer;
  private QuizQuestion.QuestionType questionType = QuizQuestion.QuestionType.SPELLING;
  private ReviewPoint reviewPoint;

  public AnswerBuilder(MakeMe makeMe) {
    super(makeMe, null);
    answer = new Answer();
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (answer.getQuestion() == null) {
      answer.setQuestion(makeMe.aQuestion().buildValid(questionType, reviewPoint).inMemoryPlease());
    }
    if (answer.getQuestion() == null)
      throw new RuntimeException(
          "Failed to generate a question of type "
              + questionType.name()
              + ", perhaps no enough data.");
    this.entity = makeMe.modelFactoryService.toAnswerModel(answer).getAnswerViewedByUser();
  }

  public AnswerBuilder forReviewPoint(ReviewPoint reviewPoint) {
    this.reviewPoint = reviewPoint;
    return this;
  }

  public AnswerBuilder type(QuizQuestion.QuestionType questionType) {
    this.questionType = questionType;
    return this;
  }

  public AnswerBuilder answerWithSpelling(String answer) {
    this.answer.setSpellingAnswer(answer);
    return this;
  }

  public AnswerBuilder answerWithId(Integer id) {
    this.answer.setAnswerNoteId(id);
    return this;
  }
}
