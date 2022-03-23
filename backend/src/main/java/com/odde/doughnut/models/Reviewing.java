package com.odde.doughnut.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.entities.json.RepetitionForUser;
import com.odde.doughnut.entities.json.ReviewPointViewedByUser;
import com.odde.doughnut.entities.json.ReviewStatus;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Reviewing {
    private final UserModel userModel;
    private final Timestamp currentUTCTimestamp;
    private final ModelFactoryService modelFactoryService;

    public Reviewing(UserModel user, Timestamp currentUTCTimestamp, ModelFactoryService modelFactoryService) {
        userModel = user;
        this.currentUTCTimestamp = currentUTCTimestamp;
        this.modelFactoryService = modelFactoryService;
    }

    public ReviewPoint getOneInitialReviewPoint() {
        int count = remainingDailyNewNotesCount();
        if (count == 0) {
            return null;
        }
        List<Integer> alreadyInitialReviewed = getNewReviewPointsOfToday().stream().map(rp -> rp.getSourceNote().getId()).toList();
        return getSubscriptionModelStream()
                .filter(sub-> sub.needToLearnMoreToday(alreadyInitialReviewed))
                .map(this::getOneNewReviewPoint)
                .filter(Objects::nonNull).findFirst().orElseGet(()-> getOneNewReviewPoint(userModel));
    }

    private ReviewPoint getOneNewReviewPoint(ReviewScope reviewScope) {
        Note note = reviewScope.getNotesHaveNotBeenReviewedAtAll().stream().findFirst().orElse(null);
        Link link = reviewScope.getLinksHaveNotBeenReviewedAtAll().stream().findFirst().orElse(null);

        if (note == null && link == null) {
            return null;
        }
        if (note != null && link != null) {
            if (note.getCreatedAt().compareTo(link.getCreatedAt()) > 0) {
                note = null;
            } else {
                link = null;
            }
        }

        ReviewPoint reviewPoint = new ReviewPoint();
        reviewPoint.setNote(note);
        reviewPoint.setLink(link);
        return reviewPoint;
    }

    @JsonProperty
    public int toRepeatCount() {
        return userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp).size();
    }

    @JsonProperty
    public int learntCount() {
        return userModel.learntCount();
    }

    @JsonProperty
    public int notLearntCount() {
        Integer subscribedCount = getSubscriptionModelStream()
                .map(this::getPendingNewReviewPointCount)
                .reduce(Integer::sum).orElse(0);
        return subscribedCount + getPendingNewReviewPointCount(userModel);
    }

    private int getPendingNewReviewPointCount(ReviewScope reviewScope) {
        int noteCount = reviewScope.getNotesHaveNotBeenReviewedAtAllCount();
        int linkCount = reviewScope.getLinksHaveNotBeenReviewedAtAllCount();

        return noteCount + linkCount;
    }

    @JsonProperty
    public int toInitialReviewCount() {
        ReviewPoint oneInitialReviewPoint = getOneInitialReviewPoint();
        if (oneInitialReviewPoint == null) {
            return 0;
        }
        return Math.min(remainingDailyNewNotesCount(), notLearntCount());
    }

    public int remainingDailyNewNotesCount() {
        long sameDayCount = getNewReviewPointsOfToday().size();
        return (int) (userModel.entity.getDailyNewNotesCount() - sameDayCount);
    }

    private List<ReviewPoint> getNewReviewPointsOfToday() {
        Timestamp oneDayAgo = TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, -24);
        return userModel.getRecentReviewPoints(oneDayAgo).stream().filter(p -> userModel.isInitialReviewOnSameDay(p, currentUTCTimestamp)).toList();
    }

    public ReviewPointModel getOneReviewPointNeedToRepeat(Randomizer randomizer) {
        List<ReviewPoint> reviewPointsNeedToRepeat = userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp);
        if (reviewPointsNeedToRepeat.size() == 0) {
            return null;
        }
        ReviewPoint reviewPoint = randomizer.chooseOneRandomly(reviewPointsNeedToRepeat);
        return modelFactoryService.toReviewPointModel(reviewPoint);
    }

    private Stream<SubscriptionModel> getSubscriptionModelStream() {
        return userModel.entity.getSubscriptions().stream().map(modelFactoryService::toSubscriptionModel);
    }

    public RepetitionForUser getOneRepetitionForUser(UserModel user, Randomizer randomizer) {
        ReviewPointModel reviewPointModel = getOneReviewPointNeedToRepeat(randomizer);

        RepetitionForUser repetitionForUser = new RepetitionForUser();

        if (reviewPointModel != null) {
            repetitionForUser.setReviewPointViewedByUser(ReviewPointViewedByUser.from(reviewPointModel.getEntity(), user));
            QuizQuestion quizQuestion = reviewPointModel.generateAQuizQuestion(randomizer);
            repetitionForUser.setQuizQuestion(QuizQuestionViewedByUser.from(quizQuestion, this.modelFactoryService.noteRepository));
        }
        repetitionForUser.setToRepeatCount(toRepeatCount());
        return repetitionForUser;
    }

    public ReviewStatus getReviewStatus() {
        ReviewStatus reviewStatus = new ReviewStatus();
        reviewStatus.toRepeatCount = toRepeatCount();
        reviewStatus.learntCount = learntCount();
        reviewStatus.notLearntCount = notLearntCount();
        reviewStatus.toInitialReviewCount = toInitialReviewCount();
        reviewStatus.remainingDailyNewNotesCount = remainingDailyNewNotesCount();

        return reviewStatus;
    }
}
