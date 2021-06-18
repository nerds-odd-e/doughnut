package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AnswerBuilder  extends EntityBuilder<AnswerResult> {
    public AnswerBuilder(AnswerResult answerResult, MakeMe makeMe) {
        super(makeMe, answerResult);
        entity.setQuestionType(QuizQuestion.QuestionType.SPELLING);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {
    }

    public AnswerBuilder forReviewPoint(ReviewPoint reviewPoint) {
        entity.setReviewPoint(reviewPoint);
        return this;
    }

    public AnswerBuilder type(QuizQuestion.QuestionType questionType) {
        entity.setQuestionType(questionType);
        return this;
    }

    public AnswerBuilder answer(String answer) {
        entity.setAnswer(answer);
        return this;
    }
}
