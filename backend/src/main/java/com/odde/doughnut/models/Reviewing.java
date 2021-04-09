package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return memoizer.call("getOneInitialReviewPointEntity", this::getOneInitialReviewPointEntity_);
    }

    private ReviewPointEntity getOneInitialReviewPointEntity_() {
        int count = remainingDailyNewNotesCount();
        if (count == 0) {
            return null;
        }
        List<Integer> initialReviewedNotesOfToday = getNewReviewPointEntitiesOfToday().stream().map(rp -> rp.getSourceNote().getId()).collect(Collectors.toUnmodifiableList());
        return getSubscriptionModelStream()
                .filter(sub-> sub.needToLearnMoreToday(initialReviewedNotesOfToday))
                .map(this::getOneNewReviewPointEntity)
                .filter(Objects::nonNull).findFirst().orElseGet(()->getOneNewReviewPointEntity(userModel));
    }

    private ReviewPointEntity getOneNewReviewPointEntity(ReviewScope reviewScope) {
        Note note = reviewScope.getNotesHaveNotBeenReviewedAtAll().stream().findFirst().orElse(null);
        Link link = reviewScope.getLinksHaveNotBeenReviewedAtAll().stream().findFirst().orElse(null);

        if (note == null && link == null) {
            return null;
        }
        if (note != null && link != null) {
            if (note.getNoteContent().getCreatedDatetime().compareTo(link.getCreateAt()) > 0) {
                note = null;
            } else {
                link = null;
            }
        }

        ReviewPointEntity reviewPointEntity = new ReviewPointEntity();
        reviewPointEntity.setNote(note);
        reviewPointEntity.setLink(link);
        return reviewPointEntity;
    }

    public int toRepeatCount() {
        return memoizer.call("toRepeatCount", this::getToRepeatCount_);
    }

    private Integer getToRepeatCount_() {
        return userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp).size();
    }

    public int learntCount() {
        return memoizer.call("learntCount", this::getLearntCount_);
    }

    public int getLearntCount_() {
        return userModel.learntCount();
    }

    public int notLearntCount() {
        return memoizer.call("notLearntCount", this::getNotLearntCount_);
    }

    public int getNotLearntCount_() {
        Integer subscribedCount = getSubscriptionModelStream()
                .map(SubscriptionModel::getNotesHaveNotBeenReviewedAtAllCount)
                .reduce(Integer::sum).orElse(0);
        return subscribedCount + userModel.getNotesHaveNotBeenReviewedAtAllCount();
    }

    public int toInitialReviewCount() {
        return memoizer.call("toInitialReviewCount", this::getToInitialReviewCount_);
    }

    public int getToInitialReviewCount_() {
        ReviewPointEntity oneInitialReviewPointEntity = getOneInitialReviewPointEntity();
        if (oneInitialReviewPointEntity == null) {
            return 0;
        }
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
        return userModel.getRecentReviewPoints(oneDayAgo).stream().filter(p -> p.isInitialReviewOnSameDay(currentUTCTimestamp, userModel.getTimeZone())).collect(Collectors.toUnmodifiableList());
    }

    public ReviewPointModel getOneReviewPointNeedToRepeat(Randomizer randomizer) {
        List<ReviewPointEntity> reviewPointsNeedToRepeat = userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp);
        if (reviewPointsNeedToRepeat.size() == 0) {
            return null;
        }
        ReviewPointEntity reviewPointEntity = randomizer.chooseOneRandomly(reviewPointsNeedToRepeat);
        return modelFactoryService.toReviewPointModel(reviewPointEntity);
    }

    public ReviewSettingEntity getReviewSettingEntity(Note note) {
        if(note == null) {
            return null;
        }
        ReviewSettingEntity reviewSettingEntity = note.getMasterReviewSettingEntity();
        if (reviewSettingEntity == null) {
            reviewSettingEntity = new ReviewSettingEntity();
        }
        return reviewSettingEntity;
    }

    private Stream<SubscriptionModel> getSubscriptionModelStream() {
        return userModel.entity.getSubscriptionEntities().stream().map(modelFactoryService::toSubscriptionModel);
    }

}
