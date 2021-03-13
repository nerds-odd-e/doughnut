package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.util.Optional;

public class Reviewing {
    private final UserModel userModel;
    private final Timestamp currentUTCTimestamp;
    private final ModelFactoryService modelFactoryService;
    private final Memoizer memoizer = new Memoizer();

    public Reviewing(UserModel user, Timestamp currentUTCTimestamp, ModelFactoryService modelFactoryService) {
        userModel = user;
        this.currentUTCTimestamp = currentUTCTimestamp;
        this.modelFactoryService = modelFactoryService;
    }

    public ReviewPointEntity getOneInitialReviewPointEntity() {
        return getOneFreshNoteToReview().map(
                noteEntity -> {
                    ReviewPointEntity reviewPointEntity = new ReviewPointEntity();
                    reviewPointEntity.setNoteEntity(noteEntity);
                    return reviewPointEntity;
                }

        ).orElse(null);
    }

    public int toRepeatCount() {
        return memoizer.call("toRepeatCount", this::getToRepeatCount);
    }

    private Integer getToRepeatCount() {
        return userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp).size();
    }

    public int learntCount() {
        return memoizer.call("learntCount", this::getLearntCount);
    }

    public int getLearntCount() {
        return userModel.learntCount();
    }

    public int notLearntCount() {
        return memoizer.call("notLearntCount", this::getNotLearntCount);
    }

    public int getNotLearntCount() {
        return userModel.getNotesHaveNotBeenReviewedAtAllCount();
    }

    public int toInitialReviewCount() {
        return memoizer.call("toInitialReviewCount", this::getToInitialReviewCount);
    }

    public int getToInitialReviewCount() {
        return Math.min(remainingDailyNewNotesCount(), notLearntCount());
    }

    private Optional<NoteEntity> getOneFreshNoteToReview() {
        int count = remainingDailyNewNotesCount();
        if (count == 0) {
            return Optional.empty();
        }
        return userModel.getNotesHaveNotBeenReviewedAtAll().stream().findFirst();
    }

    public int remainingDailyNewNotesCount() {
        Timestamp oneDayAgo = TimestampOperations.addDaysToTimestamp(currentUTCTimestamp, -1);
        long sameDayCount = userModel.getRecentReviewPoints(oneDayAgo).stream().filter(p -> p.isInitialReviewOnSameDay(currentUTCTimestamp, userModel.getTimeZone())).count();
        return (int) (userModel.entity.getDailyNewNotesCount() - sameDayCount);
    }

    public ReviewPointEntity getOneReviewPointNeedToRepeat() {
        return userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp).stream()
                .findFirst()
                .orElse(null);
    }
}
