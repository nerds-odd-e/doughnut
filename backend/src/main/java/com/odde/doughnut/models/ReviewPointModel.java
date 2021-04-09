package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;

public class ReviewPointModel extends ModelForEntity<ReviewPointEntity> {
    public ReviewPointModel(ReviewPointEntity entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService);
    }

    public void initialReview(UserModel userModel, ReviewSettingEntity reviewSettingEntity, Timestamp currentUTCTimestamp) {
        Note note = entity.getNote();
        if (note != null) {
            note.mergeMasterReviewSetting(reviewSettingEntity);
            modelFactoryService.noteRepository.save(note);
        }
        entity.setUserEntity(userModel.getEntity());
        entity.setInitialReviewedAt(currentUTCTimestamp);
        repeat(currentUTCTimestamp);
    }

    public void repeat(Timestamp currentUTCTimestamp) {
        repeatWithAdjust(currentUTCTimestamp, 0);
    }

    public void repeatSad(Timestamp currentUTCTimestamp) {
        repeatWithAdjust(currentUTCTimestamp, -1);
    }

    public void repeatHappy(Timestamp currentUTCTimestamp) {
        repeatWithAdjust(currentUTCTimestamp, 1);
    }

    public void repeatWithAdjust(Timestamp currentUTCTimestamp, int adjustment) {
        entity.updateMemoryState(currentUTCTimestamp, getMemoryStateChange(adjustment));
        this.modelFactoryService.reviewPointRepository.save(entity);
    }

    private SpacedRepetitionAlgorithm.MemoryStateChange getMemoryStateChange(int adjustment) {
        return getUserModel().getSpacedRepetitionAlgorithm().getMemoryStateChange(this.entity.getForgettingCurveIndex(), adjustment);
    }

    private UserModel getUserModel() {
        return modelFactoryService.toUserModel(entity.getUserEntity());
    }

    public QuizQuestion generateAQuizQuestion(Randomizer randomizer) {
        return new QuizQuestionGenerator(entity, randomizer).generateQuestion(modelFactoryService);
    }

}
