package com.odde.doughnut.models;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.util.List;
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
        int count = remainingDailyNewNotesCount();
        if (count == 0) {
            return null;
        }

        ReviewPointEntity reviewPointEntity1 = getOneFreshNoteToReview().map(
                noteEntity -> {
                    ReviewPointEntity reviewPointEntity = new ReviewPointEntity();
                    reviewPointEntity.setNoteEntity(noteEntity);
                    return reviewPointEntity;
                }
        ).orElse(null);

        ReviewPointEntity reviewPointEntity2 = getOneFreshLinkToReview().map(
                linkEntity -> {
                    ReviewPointEntity reviewPointEntity = new ReviewPointEntity();
                    reviewPointEntity.setLinkEntity(linkEntity);
                    return reviewPointEntity;
                }
        ).orElse(null);

        if (reviewPointEntity1 == null) {
            return reviewPointEntity2;
        }

        if (reviewPointEntity2 == null) {
            return reviewPointEntity1;
        }

        if(reviewPointEntity1.getNoteEntity().getCreatedDatetime().compareTo(reviewPointEntity2.getLinkEntity().getCreateAt()) > 0) {
            return reviewPointEntity2;
        }


        return reviewPointEntity1;
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
        return userModel.getNotesHaveNotBeenReviewedAtAll().stream().findFirst();
    }

    private Optional<LinkEntity> getOneFreshLinkToReview() {
        return userModel.getLinksHaveNotBeenReviewedAtAll().stream().findFirst();
    }

    public int remainingDailyNewNotesCount() {
        Timestamp oneDayAgo = TimestampOperations.addDaysToTimestamp(currentUTCTimestamp, -1);
        long sameDayCount = userModel.getRecentReviewPoints(oneDayAgo).stream().filter(p -> p.isInitialReviewOnSameDay(currentUTCTimestamp, userModel.getTimeZone())).count();
        return (int) (userModel.entity.getDailyNewNotesCount() - sameDayCount);
    }

    public ReviewPointModel getOneReviewPointNeedToRepeat(Randomizer randomizer) {
        List<ReviewPointEntity> reviewPointsNeedToRepeat = userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp);
        if (reviewPointsNeedToRepeat.size() == 0) {
            return null;
        }
        ReviewPointEntity reviewPointEntity = randomizer.chooseOneRandomly(reviewPointsNeedToRepeat);
        return modelFactoryService.toReviewPointModel(reviewPointEntity);
    }

    public ReviewSettingEntity getReviewSettingEntity(NoteEntity noteEntity) {
        if(noteEntity == null) {
            return null;
        }
        ReviewSettingEntity reviewSettingEntity = noteEntity.getMasterReviewSettingEntity();
        if (reviewSettingEntity == null) {
            reviewSettingEntity = new ReviewSettingEntity();
        }
        return reviewSettingEntity;
    }
}
