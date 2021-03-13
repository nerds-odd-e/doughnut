package com.odde.doughnut.models;

import com.odde.doughnut.entities.CircleEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
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
        return entity.orphanedNotes();
    }

    public void assertAuthorization(NoteEntity noteEntity) throws NoAccessRightException {
        if (!hasAuthority(noteEntity)) {
            throw new NoAccessRightException();
        }
    }

    public boolean hasAuthority(NoteEntity noteEntity) {
        return entity.owns(noteEntity);
    }

    public void assertAuthorization(CircleEntity circleEntity) throws NoAccessRightException {
        if(!inCircle(circleEntity)) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(UserEntity userEntity) throws NoAccessRightException {
        if(entity.getId() != userEntity.getId()) {
            throw new NoAccessRightException();
        }
    }
    public boolean inCircle(CircleEntity circleEntity) {
        return circleEntity.getMembers().contains(entity);
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

    public List<NoteEntity> getNotesHaveNotBeenReviewedAtAll() {
        return modelFactoryService.noteRepository.findByUserWhereThereIsNoReviewPoint(entity.getId());
    }

    public int getNotesHaveNotBeenReviewedAtAllCount() {
        return modelFactoryService.noteRepository.countByUserWhereThereIsNoReviewPoint(entity.getId());
    }

    public List<ReviewPointEntity> getRecentReviewPoints(Timestamp since) {
        return modelFactoryService.reviewPointRepository.findAllByUserEntityAndInitialReviewedAtGreaterThan(entity, since);
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

    public List<ReviewPointEntity> getReviewPointsNeedToRepeat(Timestamp currentUTCTimestamp) {
        return modelFactoryService.reviewPointRepository
                .findAllByUserEntityAndNextReviewAtLessThanEqualOrderByNextReviewAt(
                        getEntity(),
                        currentUTCTimestamp
                );
    }

    public SpacedRepetition getSpacedRepetition() {
        return new SpacedRepetition(entity.getSpaceIntervals());
    }

    public NoteEntity newNote(NoteEntity parentNote) {
        NoteEntity noteEntity = new NoteEntity();
        noteEntity.setParentNote(parentNote);
        if (parentNote != null) {
            noteEntity.setOwnershipEntity(parentNote.getOwnershipEntity());
        }
        else {
            noteEntity.setOwnershipEntity(getEntity().getOwnershipEntity());
        }
        return noteEntity;
    }

    int learntCount() {
        return modelFactoryService.reviewPointRepository.countByUserEntityNotRemoved(entity);
    }

}
