package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SpacedRepetition;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;

public class ReviewPointModel extends ModelForEntity<ReviewPointEntity> {
    public ReviewPointModel(ReviewPointEntity entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService);
    }

    public void initialReview(UserModel userModel, ReviewSettingEntity reviewSettingEntity, Timestamp currentUTCTimestamp) {
        if (getNoteModel() != null) {
            getNoteModel().setAndSaveMasterReviewSetting(reviewSettingEntity);
        }
        entity.setUserEntity(userModel.getEntity());
        entity.setInitialReviewedAt(currentUTCTimestamp);
        repeat(currentUTCTimestamp);
    }

    public void repeat(Timestamp currentUTCTimestamp) {
        if (entity.getRepeatAgainToday()) {
            entity.setNextReviewAt(currentUTCTimestamp);
        }
        else {
            entity.setLastReviewedAt(currentUTCTimestamp);
            entity.setForgettingCurveIndex(getSpacedRepetition().getNextForgettingCurveIndex(entity.getForgettingCurveIndex()));
            entity.setNextReviewAt(calculateNextReviewAt(getSpacedRepetition()));
        }
        this.modelFactoryService.reviewPointRepository.save(entity);
    }

    private SpacedRepetition getSpacedRepetition() {
        return getUserModel().getSpacedRepetition();
    }

    private UserModel getUserModel() {
        return modelFactoryService.toUserModel(entity.getUserEntity());
    }

    private NoteContentModel getNoteModel() {
        return modelFactoryService.toNoteModel(entity.getNoteEntity());
    }

    private Timestamp calculateNextReviewAt(SpacedRepetition spacedRepetition) {
        return TimestampOperations.addDaysToTimestamp(entity.getLastReviewedAt(), spacedRepetition.getNextRepeatInDays(entity.getForgettingCurveIndex()));
    }

    public QuizQuestion generateAQuizQuestion(Randomizer randomizer) {
        return new QuizQuestionGenerator(entity, randomizer).generateQuestion(randomizer, entity, modelFactoryService);
    }

}
