package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class UserModel extends ModelForEntity<UserEntity> implements ReviewScope {
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

    public void assertAuthorization(NoteEntity noteEntity) throws NoAccessRightException {
        if (!hasFullAuthority(noteEntity)) {
            throw new NoAccessRightException();
        }
    }

    public boolean hasFullAuthority(NoteEntity noteEntity) {
        return hasFullAuthority(noteEntity.getNotebookEntity());
    }

    public boolean hasFullAuthority(NotebookEntity notebookEntity) {
        return entity.owns(notebookEntity);
    }

    public boolean hasReadAuthority(NoteEntity noteEntity) {
        if(hasFullAuthority(noteEntity)) return true;

        return entity.getSubscriptionEntities().stream().anyMatch(s->s.getNotebookEntity() == noteEntity.getNotebookEntity());
    }

    public void assertReadAuthorization(NoteEntity noteEntity) throws NoAccessRightException {
        if (!hasReadAuthority(noteEntity)) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(NotebookEntity notebookEntity) throws NoAccessRightException {
        if (!hasFullAuthority(notebookEntity)) {
            throw new NoAccessRightException();
        }
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

    public void assertAuthorization(LinkEntity linkEntity) throws NoAccessRightException {
        if(linkEntity.getUserEntity().getId() != entity.getId()) {
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

    @Override
    public List<NoteEntity> getNotesHaveNotBeenReviewedAtAll() {
        return modelFactoryService.noteRepository.findByOwnershipWhereThereIsNoReviewPoint(entity);
    }

    @Override
    public int getNotesHaveNotBeenReviewedAtAllCount() {
        return modelFactoryService.noteRepository.countByOwnershipWhereThereIsNoReviewPoint(entity);
    }

    @Override
    public List<LinkEntity> getLinksHaveNotBeenReviewedAtAll() {
        return modelFactoryService.linkRepository.findByOwnershipWhereThereIsNoReviewPoint(entity);
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

    public SpacedRepetitionAlgorithm getSpacedRepetitionAlgorithm() {
        return new SpacedRepetitionAlgorithm(entity.getSpaceIntervals());
    }

    int learntCount() {
        return modelFactoryService.reviewPointRepository.countByUserEntityNotRemoved(entity);
    }

    public Reviewing createReviewing(Timestamp currentUTCTimestamp) {
        return new Reviewing(this, currentUTCTimestamp, modelFactoryService);
    }

}
