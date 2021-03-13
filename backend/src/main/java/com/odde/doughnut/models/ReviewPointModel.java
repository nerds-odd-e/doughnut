package com.odde.doughnut.models;

import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.sql.Timestamp;

public class ReviewPointModel extends ModelForEntity<ReviewPointEntity> {
    public ReviewPointModel(ReviewPointEntity entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService);
    }

    public void initialReview(UserModel userModel, Timestamp currentUTCTimestamp) {
        getEntity().setUserEntity(userModel.getEntity());
        getEntity().setInitialReviewedAt(currentUTCTimestamp);
        repeat(currentUTCTimestamp);
    }

    public void repeat(Timestamp currentUTCTimestamp) {
        getEntity().setForgettingCurveIndex(getSpacedRepetition().getNextForgettingCurveIndex(getEntity().getForgettingCurveIndex()));
        getEntity().setLastReviewedAt(currentUTCTimestamp);
        getEntity().setNextReviewAt(calculateNextReviewAt(getSpacedRepetition()));
        this.modelFactoryService.reviewPointRepository.save(getEntity());
    }

    private SpacedRepetition getSpacedRepetition() {
        return getUserModel().getSpacedRepetition();
    }

    private UserModel getUserModel() {
        return modelFactoryService.toUserModel(getEntity().getUserEntity());
    }

    private Timestamp calculateNextReviewAt(SpacedRepetition spacedRepetition) {
        return TimestampOperations.addDaysToTimestamp(getEntity().getLastReviewedAt(), spacedRepetition.getNextRepeatInDays(getEntity().getForgettingCurveIndex()));
    }

    public QuizQuestion generateAQuizQuestion() {
        if (Strings.isEmpty(getEntity().getNoteEntity().getDescription())) {
            return null;
        }
        return new QuizQuestion(entity.getNoteEntity(), modelFactoryService);
    }
}
