package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;

import java.sql.Timestamp;
import java.util.Optional;

public class Reviewing {
    private final UserModel userModel;
    private final Timestamp currentUTCTimestamp;

    public Reviewing(UserModel user, Timestamp currentUTCTimestamp) {

        userModel = user;
        this.currentUTCTimestamp = currentUTCTimestamp;
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
        return userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp).size();
    }

    public int learntCount() {
        return userModel.learntCount();
    }

    public int notLearntCount() {
        return userModel.getNotesHaveNotBeenReviewedAtAllCount();
    }

    public int toInitialReviewCount() {
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
        long sameDayCount = userModel.getRecentReviewPoints1(oneDayAgo).stream().filter(p -> p.isInitialReviewOnSameDay(currentUTCTimestamp, userModel.getTimeZone())).count();
        return (int) (userModel.entity.getDailyNewNotesCount() - sameDayCount);
    }

    public ReviewPointEntity getOneReviewPointNeedToRepeat() {
        return userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp).stream().findFirst().orElse(null);
    }
}
