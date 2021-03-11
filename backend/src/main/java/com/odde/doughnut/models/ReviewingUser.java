package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;

import java.sql.Timestamp;
import java.util.Optional;

public class ReviewingUser {
    public final UserModel userModel;
    private final Timestamp currentUTCTimestamp;

    public ReviewingUser(UserModel user, Timestamp currentUTCTimestamp) {

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

    private Optional<NoteEntity> getOneFreshNoteToReview() {
        int count = getNewNotesCountForToday();
        if (count == 0) {
            return Optional.empty();
        }
        return userModel.getNotesHaveNotBeenReviewedAtAll().stream().findFirst();
    }

    private int getNewNotesCountForToday() {
        return userModel.getNewNotesCountForToday(currentUTCTimestamp);
    }
}
