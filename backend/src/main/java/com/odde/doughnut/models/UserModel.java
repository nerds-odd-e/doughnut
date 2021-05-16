package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

public class UserModel implements ReviewScope {

    @Getter
    protected final User entity;
    protected final ModelFactoryService modelFactoryService;

    public UserModel(User user, ModelFactoryService modelFactoryService) {
        this.entity = user;
        this.modelFactoryService = modelFactoryService;
    }

    public Authorization getAuthorization() {
        return modelFactoryService.toAuthorization(entity);
    }

    public String getName() {
        return entity.getName();
    }

    public void setAndSaveDailyNewNotesCount(Integer dailyNewNotesCount) {
        entity.setDailyNewNotesCount(dailyNewNotesCount);
        save();
    }

    public void setAndSaveSpaceIntervals(String spaceIntervals) {
        entity.setSpaceIntervals(spaceIntervals);
        save();
    }

    public List<Note> getLinkableNotes(Note note, SearchTerm searchTerm) {
        if (Strings.isBlank(searchTerm.getSearchKey())) {
            return null;
        }
        final String pattern = Pattern.quote(searchTerm.getSearchKey());
        if (searchTerm.getSearchGlobally()) {
            return  modelFactoryService.noteRepository.searchForUserInVisibleScope(entity, note, pattern);
        }
        return  modelFactoryService.noteRepository.searchInNotebook(note.getNotebook(), note, pattern);

    }

    @Override
    public List<Note> getNotesHaveNotBeenReviewedAtAll() {
        return modelFactoryService.noteRepository.findByOwnershipWhereThereIsNoReviewPoint(entity);
    }

    @Override
    public int getNotesHaveNotBeenReviewedAtAllCount() {
        return modelFactoryService.noteRepository.countByOwnershipWhereThereIsNoReviewPoint(entity);
    }

    @Override
    public List<Link> getLinksHaveNotBeenReviewedAtAll() {
        return modelFactoryService.linkRepository.findByOwnershipWhereThereIsNoReviewPoint(entity);
    }

    public List<ReviewPoint> getRecentReviewPoints(Timestamp since) {
        return modelFactoryService.reviewPointRepository.findAllByUserAndInitialReviewedAtGreaterThan(entity, since);
    }

    public List<ReviewPoint> getReviewPointsNeedToRepeat(Timestamp currentUTCTimestamp) {
        return modelFactoryService.reviewPointRepository
                .findAllByUserAndNextReviewAtLessThanEqualOrderByNextReviewAt(
                        getEntity(),
                        currentUTCTimestamp
                );
    }

    public SpacedRepetitionAlgorithm getSpacedRepetitionAlgorithm() {
        return new SpacedRepetitionAlgorithm(entity.getSpaceIntervals());
    }

    int learntCount() {
        return modelFactoryService.reviewPointRepository.countByUserNotRemoved(entity);
    }

    public Reviewing createReviewing(Timestamp currentUTCTimestamp) {
        return new Reviewing(this, currentUTCTimestamp, modelFactoryService);
    }

    private void save() {
        modelFactoryService.entityManager.persist(entity);
    }

    boolean isInitialReviewOnSameDay(ReviewPoint reviewPoint, Timestamp currentUTCTimestamp) {
        return reviewPoint.isInitialReviewOnSameDay(currentUTCTimestamp, getTimeZone());
    }

    public ZoneId getTimeZone() {
        return ZoneId.of("Asia/Shanghai");
    }

}
