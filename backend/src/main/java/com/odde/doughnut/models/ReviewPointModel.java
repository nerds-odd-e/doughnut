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

    public int repeated(Timestamp currentUTCTimestamp) {
        return updateNextRepetitionWithAdjustment(currentUTCTimestamp, 0);
    }

    public int repeatedSad(Timestamp currentUTCTimestamp) {
        return updateNextRepetitionWithAdjustment(currentUTCTimestamp, -1);
    }

    public int repeatedHappy(Timestamp currentUTCTimestamp) {
        return updateNextRepetitionWithAdjustment(currentUTCTimestamp, 1);
    }

    private int updateNextRepetitionWithAdjustment(Timestamp currentUTCTimestamp, int adjustment) {
        entity.updateMemoryState(currentUTCTimestamp, getMemoryStateChange(adjustment));
        return increaseRepetitionCountAndSave();
    }

    public int increaseRepetitionCountAndSave() {
        final int repetitionCount = entity.getRepetitionCount() + 1;
        entity.setRepetitionCount(repetitionCount);
        this.modelFactoryService.reviewPointRepository.save(entity);
        return repetitionCount;
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
