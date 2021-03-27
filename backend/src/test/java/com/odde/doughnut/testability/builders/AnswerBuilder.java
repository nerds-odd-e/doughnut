package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AnswerEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.AnswerModel;
import com.odde.doughnut.models.QuizQuestion;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AnswerBuilder  extends EntityBuilder<AnswerEntity> {
    public AnswerBuilder(AnswerEntity answerEntity, MakeMe makeMe) {
        super(makeMe, answerEntity);
        entity.setQuestionType(QuizQuestion.QuestionType.SPELLING);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {
    }

    public AnswerBuilder forReviewPoint(ReviewPointEntity reviewPointEntity) {
        entity.setReviewPointEntity(reviewPointEntity);
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
