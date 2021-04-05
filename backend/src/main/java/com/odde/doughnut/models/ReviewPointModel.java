package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;

public class ReviewPointModel extends ModelForEntity<ReviewPointEntity> {
    public ReviewPointModel(ReviewPointEntity entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService);
    }

    public void initialReview(UserModel userModel, ReviewSettingEntity reviewSettingEntity, Timestamp currentUTCTimestamp) {
        NoteEntity noteEntity = entity.getNoteEntity();
        if (noteEntity != null) {
            noteEntity.mergeMasterReviewSetting(reviewSettingEntity);
            modelFactoryService.noteRepository.save(noteEntity);
        }
        entity.setUserEntity(userModel.getEntity());
        entity.setInitialReviewedAt(currentUTCTimestamp);
        repeat(currentUTCTimestamp);
    }

    public void repeat(Timestamp currentUTCTimestamp) {
        entity.updateMemoryState(currentUTCTimestamp, getMemoryStateChange());
        this.modelFactoryService.reviewPointRepository.save(entity);
    }

    public void repeatSad(Timestamp currentUTCTimestamp) {
        repeat(currentUTCTimestamp);
    }

    public void repeatHappy(Timestamp currentUTCTimestamp) {
        repeat(currentUTCTimestamp);
    }

    private SpacedRepetitionAlgorithm.MemoryStateChange getMemoryStateChange() {
        return getUserModel().getSpacedRepetitionAlgorithm().getMemoryStateChange(this.entity.getForgettingCurveIndex());
    }

    private UserModel getUserModel() {
        return modelFactoryService.toUserModel(entity.getUserEntity());
    }

    public QuizQuestion generateAQuizQuestion(Randomizer randomizer) {
        return new QuizQuestionGenerator(entity, randomizer).generateQuestion(modelFactoryService);
    }

}
