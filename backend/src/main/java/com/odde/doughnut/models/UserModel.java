package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import javax.validation.Valid;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class UserModel extends ModelForEntity<User> implements ReviewScope {
    public UserModel(User user, ModelFactoryService modelFactoryService) {
        super(user, modelFactoryService);
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

    public List<Note> filterLinkableNotes(Note note, @Valid SearchTerm searchTerm) {
        if (Strings.isBlank(searchTerm.getSearchKey())) {
            return null;
        }
        final String searchKey = searchTerm.getSearchKey().toLowerCase();
        List<Note> linkableNotes;
        if (searchTerm.getSearchGlobally()) {
            linkableNotes = entity.getNotes();
        }
        else {
            linkableNotes = note.getNotebook().getNotes();
        }

        return linkableNotes.stream()
                .filter(n -> !n.equals(note))
                .filter(n -> n.getTitle().toLowerCase().contains(searchKey))
                .collect(Collectors.toList());
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

    public ZoneId getTimeZone() {
        return ZoneId.of("Asia/Shanghai");
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

}
