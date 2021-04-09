package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;

public class ReviewPointModel extends ModelForEntity<ReviewPoint> {
    public ReviewPointModel(ReviewPoint entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService);
    }

    public void initialReview(UserModel userModel, ReviewSetting reviewSetting, Timestamp currentUTCTimestamp) {
        Note note = entity.getNote();
        if (note != null) {
            note.mergeMasterReviewSetting(reviewSetting);
            modelFactoryService.noteRepository.save(note);
        }
        entity.setUser(userModel.getEntity());
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
        return modelFactoryService.toUserModel(entity.getUser());
    }

    public QuizQuestion generateAQuizQuestion(Randomizer randomizer) {
        return new QuizQuestionGenerator(entity, randomizer).generateQuestion(modelFactoryService);
    }

}
