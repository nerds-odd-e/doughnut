package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.sql.Timestamp;

public record ReviewPointModel(ReviewPoint entity,
                               ModelFactoryService modelFactoryService) {
    public ReviewPoint getEntity() {
        return entity;
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
        entity.changeNextRepetitionWithAdjustment(currentUTCTimestamp, adjustment);
        this.modelFactoryService.reviewPointRepository.save(entity);
    }

    public void increaseRepetitionCountAndSave() {
        final int repetitionCount = entity.getRepetitionCount() + 1;
        entity.setRepetitionCount(repetitionCount);
        this.modelFactoryService.reviewPointRepository.save(entity);
    }

    public QuizQuestion generateAQuizQuestion(Randomizer randomizer) {
        return new QuizQuestionGenerator(entity, randomizer).generateQuestion(modelFactoryService);
    }

}
