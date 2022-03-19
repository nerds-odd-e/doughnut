package com.odde.doughnut.models;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.SearchTerm;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import org.apache.logging.log4j.util.Strings;

import lombok.Getter;

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

    public boolean loggedIn() {
        return entity != null;
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

    public List<Note> searchForNotes(SearchTerm searchTerm) {
        if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
            return null;
        }
        List<Note> result = search(searchTerm);

        Integer avoidNoteId = searchTerm.note.map(Note::getId).orElse(null);
        return result.stream().filter(n -> !n.getId().equals(avoidNoteId)).collect(Collectors.toList());

    }

    private List<Note> search(SearchTerm searchTerm) {
        final String pattern = Pattern.quote(searchTerm.getTrimmedSearchKey());
        if (searchTerm.getAllMyNotebooksAndSubscriptions()) {
            return modelFactoryService.noteRepository.searchForUserInVisibleScope(entity, pattern);
        }
        Notebook notebook = searchTerm.note.map(Note::getNotebook).orElse(null);
        return modelFactoryService.noteRepository.searchInNotebook(notebook, pattern);
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
        final ZoneId timeZone = getTimeZone();
        final Timestamp timestamp = TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone);
        return modelFactoryService.reviewPointRepository
                .findAllByUserAndNextReviewAtLessThanEqualOrderByNextReviewAt(
                        getEntity(),
                        timestamp
                );
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

    public ReviewPoint getReviewPointFor(Note note) {
        return modelFactoryService.reviewPointRepository.findByUserAndNote(entity, note);
    }

    public ReviewPoint getReviewPointFor(Link link) {
        return modelFactoryService.reviewPointRepository.findByUserAndLink(entity, link);
    }
}
