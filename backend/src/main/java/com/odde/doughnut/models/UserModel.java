package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserModel extends ModelForEntity<UserEntity> {
    public UserModel(UserEntity userEntity, ModelFactoryService modelFactoryService) {
        super(userEntity, modelFactoryService);
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

    public List<NoteEntity> getOrphanedNotes() {
        return entity.getOrphanedNotes();
    }

    public void assertAuthorization(NoteEntity noteEntity) throws NoAccessRightException {
        if (!entity.owns(noteEntity)) {
            throw new NoAccessRightException();
        }
    }

    public List<NoteEntity> filterLinkableNotes(NoteEntity noteEntity, String searchTerm) {
        List<NoteEntity> linkableNotes = getAllLinkableNotes(noteEntity);
        if (searchTerm != null) {
            return linkableNotes.stream()
                    .filter(note -> note.getTitle().contains(searchTerm))
                    .collect(Collectors.toList());
        }
        return linkableNotes;
    }

    public ReviewPointEntity getOneInitialReviewPointEntity(Timestamp currentUTCTimestamp) {
        return getOneFreshNoteToReview(currentUTCTimestamp).map(
                noteEntity -> {
                    ReviewPointEntity reviewPointEntity = new ReviewPointEntity();
                    reviewPointEntity.setNoteEntity(noteEntity);
                    return reviewPointEntity;
                }

        ).orElse(null);
    }

    private Optional<NoteEntity> getOneFreshNoteToReview(Timestamp currentTime) {
        int count = getNewNotesCountForToday(currentTime);
        if (count == 0) {
            return Optional.empty();
        }
        List<NoteEntity> notesHaveNotBeenReviewedAtAll = getNotesHaveNotBeenReviewedAtAll();
        return notesHaveNotBeenReviewedAtAll.stream().findFirst();
    }

    private List<NoteEntity> getNotesHaveNotBeenReviewedAtAll() {
        return modelFactoryService.noteRepository.findByUserWhereThereIsNoReviewPoint(entity.getId());
    }

    private List<ReviewPointEntity> getRecentReviewPoints(Timestamp currentTime) {
        Timestamp oneDayAgo = addDaysToTimestamp(currentTime, -1);
        return modelFactoryService.reviewPointRepository.findAllByUserEntityAndInitialReviewedAtGreaterThan(entity, oneDayAgo);
    }

    public static Timestamp addDaysToTimestamp(Timestamp timestamp, int daysToAdd) {
        ZonedDateTime zonedDateTime = timestamp.toInstant().atZone(ZoneId.of("UTC"));
        return Timestamp.from(zonedDateTime.plus(daysToAdd, ChronoUnit.DAYS).toInstant());
    }

    private int getNewNotesCountForToday(Timestamp currentTime) {
        long sameDayCount = getRecentReviewPoints(currentTime).stream().filter(p -> p.isInitialReviewOnSameDay(currentTime, getTimeZone())).count();
        return (int) (entity.getDailyNewNotesCount() - sameDayCount);
    }

    private List<NoteEntity> getAllLinkableNotes(NoteEntity source) {
        List<NoteEntity> targetNotes = source.getTargetNotes();
        List<NoteEntity> allNotes = entity.getNotes();
        return allNotes.stream()
                .filter(i -> !targetNotes.contains(i))
                .filter(i -> !i.equals(source))
                .collect(Collectors.toList());
    }

    public ZoneId getTimeZone() {
        return ZoneId.of("Asia/Shanghai");
    }

    public ReviewPointEntity getReviewPointNeedToRepeat(Timestamp currentUTCTimestamp) {
        return modelFactoryService.reviewPointRepository.findAllByUserEntityOrderByLastReviewedAt(getEntity()).stream().filter(
                reviewPointEntity -> {
                    Timestamp lastReviewedAt = reviewPointEntity.getLastReviewedAt();
                    Timestamp nextReviewTime = addDaysToTimestamp(lastReviewedAt, getSpacedRepetition().getNextRepeatInDays(reviewPointEntity.getForgettingCurveIndex()));
                    return (nextReviewTime.compareTo(currentUTCTimestamp) != 1);
                }
        ).findFirst().orElse(null) ;
    }

    private SpacedRepetition getSpacedRepetition() {
        return new SpacedRepetition(entity.getSpaceIntervals());
    }

}
