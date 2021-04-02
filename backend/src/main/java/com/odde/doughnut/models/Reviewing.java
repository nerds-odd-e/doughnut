package com.odde.doughnut.models;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        List<Integer> initialReviewedNotesOfToday = getNewReviewPointEntitiesOfToday().stream().map(rp -> rp.getSourceNote().getId()).collect(Collectors.toUnmodifiableList());
        return userModel.entity.getSubscriptionEntities().stream().map(modelFactoryService::toSubscriptionModel)
        .filter(sub->{
           return sub.needToLearnMoreToday(initialReviewedNotesOfToday);
        }).map(this::getOneNewReviewPointEntity).filter(Objects::nonNull).findFirst().orElse(getOneNewReviewPointEntity(userModel));
    }

    private ReviewPointEntity getOneNewReviewPointEntity(ReviewScope reviewScope) {
        NoteEntity noteEntity = reviewScope.getNotesHaveNotBeenReviewedAtAll().stream().findFirst().orElse(null);
        LinkEntity linkEntity = reviewScope.getLinksHaveNotBeenReviewedAtAll().stream().findFirst().orElse(null);

        if (noteEntity == null && linkEntity == null) {
            return null;
        }
        if (noteEntity != null && linkEntity != null) {
            if (noteEntity.getNoteContent().getCreatedDatetime().compareTo(linkEntity.getCreateAt()) > 0) {
                noteEntity = null;
            } else {
                linkEntity = null;
            }
        }

        ReviewPointEntity reviewPointEntity = new ReviewPointEntity();
        reviewPointEntity.setNoteEntity(noteEntity);
        reviewPointEntity.setLinkEntity(linkEntity);
        return reviewPointEntity;
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

    public int remainingDailyNewNotesCount() {
        long sameDayCount = getNewReviewPointEntitiesOfToday().size();
        return (int) (userModel.entity.getDailyNewNotesCount() - sameDayCount);
    }

    private List<ReviewPointEntity> getNewReviewPointEntitiesOfToday() {
        return memoizer.call("getNewReviewPointEntitiesOfToday", this::getNewReviewPointEntitiesOfToday_);
    }

    private List<ReviewPointEntity> getNewReviewPointEntitiesOfToday_() {
        Timestamp oneDayAgo = TimestampOperations.addDaysToTimestamp(currentUTCTimestamp, -1);
        List<ReviewPointEntity> collect = userModel.getRecentReviewPoints(oneDayAgo).stream().filter(p -> p.isInitialReviewOnSameDay(currentUTCTimestamp, userModel.getTimeZone())).collect(Collectors.toUnmodifiableList());
        return collect;
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
