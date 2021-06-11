package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.Getter;

import java.sql.Timestamp;

public class ReviewPointModel {
    @Getter
    protected final ReviewPoint entity;
    protected final ModelFactoryService modelFactoryService;

    public ReviewPointModel(ReviewPoint entity, ModelFactoryService modelFactoryService) {
        this.entity = entity;
        this.modelFactoryService = modelFactoryService;
    }

    public void initialReview(UserModel userModel, ReviewSetting reviewSetting, Timestamp currentUTCTimestamp) {
        Note note = entity.getNote();
        if (note != null) {
            note.mergeMasterReviewSetting(reviewSetting);
            modelFactoryService.noteRepository.save(note);
        }
        entity.setUser(userModel.getEntity());
        entity.setInitialReviewedAt(currentUTCTimestamp);
        repeated(currentUTCTimestamp);
    }

    public void repeated(Timestamp currentUTCTimestamp) {
        updateNextRepetitionWithAdjustment(currentUTCTimestamp, 0);
    }

    public void repeatedSad(Timestamp currentUTCTimestamp) {
        updateNextRepetitionWithAdjustment(currentUTCTimestamp, -1);
    }

    public void repeatedHappy(Timestamp currentUTCTimestamp) {
        updateNextRepetitionWithAdjustment(currentUTCTimestamp, 1);
    }

    private void updateNextRepetitionWithAdjustment(Timestamp currentUTCTimestamp, int adjustment) {
        entity.updateMemoryState(currentUTCTimestamp, getMemoryStateChange(adjustment));
        increaseRepetitionCountAndSave();
    }

    public void increaseRepetitionCountAndSave() {
        entity.setRepetitionCount(entity.getRepetitionCount() + 1);
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
