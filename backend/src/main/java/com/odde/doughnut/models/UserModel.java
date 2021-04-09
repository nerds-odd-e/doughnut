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

    public void assertAuthorization(Note note) throws NoAccessRightException {
        if (!hasFullAuthority(note)) {
            throw new NoAccessRightException();
        }
    }

    public boolean hasFullAuthority(Note note) {
        return hasFullAuthority(note.getNotebook());
    }

    public boolean hasFullAuthority(Notebook notebook) {
        return entity.owns(notebook);
    }

    public boolean hasReadAuthority(Note note) {
        if(hasFullAuthority(note)) return true;

        return entity.getSubscriptionEntities().stream().anyMatch(s->s.getNotebook() == note.getNotebook());
    }

    public void assertReadAuthorization(Note note) throws NoAccessRightException {
        if (!hasReadAuthority(note)) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(Notebook notebook) throws NoAccessRightException {
        if (!hasFullAuthority(notebook)) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(Circle circle) throws NoAccessRightException {
        if(!inCircle(circle)) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(UserEntity userEntity) throws NoAccessRightException {
        if(entity.getId() != userEntity.getId()) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(Link link) throws NoAccessRightException {
        if(link.getUserEntity().getId() != entity.getId()) {
            throw new NoAccessRightException();
        }
    }

    public boolean inCircle(Circle circle) {
        return circle.getMembers().contains(entity);
    }

    public List<Note> filterLinkableNotes(Note note, String searchTerm) {
        List<Note> linkableNotes = getAllLinkableNotes(note);
        if (searchTerm != null) {
            return linkableNotes.stream()
                    .filter(n -> n.getTitle().contains(searchTerm))
                    .collect(Collectors.toList());
        }
        return linkableNotes;
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

    public List<ReviewPointEntity> getRecentReviewPoints(Timestamp since) {
        return modelFactoryService.reviewPointRepository.findAllByUserEntityAndInitialReviewedAtGreaterThan(entity, since);
    }

    private List<Note> getAllLinkableNotes(Note source) {
        List<Note> targetNotes = source.getTargetNotes();
        List<Note> allNotes = entity.getNotes();
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
