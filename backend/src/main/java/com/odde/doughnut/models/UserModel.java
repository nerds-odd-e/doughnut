package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class UserModel {

    @Getter
    private final UserEntity userEntity;
    private final ModelFactoryService modelFactoryService;

    public UserModel(UserEntity userEntity, ModelFactoryService modelFactoryService) {
        this.userEntity = userEntity;
        this.modelFactoryService = modelFactoryService;
    }

    public String getName() {
        return userEntity.getName();
    }

    public long getDailyNewNotesCount() {
        return userEntity.getDailyNewNotesCount();
    }

    public void setDailyNewNotesCount(Integer dailyNewNotesCount) {
        userEntity.setDailyNewNotesCount(dailyNewNotesCount);
        save();
    }

    public void setSpaceIntervals(String spaceIntervals) {
        userEntity.setSpaceIntervals(spaceIntervals);
        save();
    }

    private void save() {
        modelFactoryService.userRepository.save(userEntity);
    }

    public List<NoteEntity> getOrphanedNotes() {
        return userEntity.getOrphanedNotes();
    }

    public void assertAuthorization(NoteEntity noteEntity) throws NoAccessRightException {
        if (! userEntity.owns(noteEntity)) {
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

    public List<NoteEntity> getNewNotesToReview() {
        List<NoteEntity> notes = userEntity.getNotes();
        notes.sort(Comparator.comparing(NoteEntity::getUpdatedDatetime).reversed());
        return notes;
    }

    private List<NoteEntity> getAllLinkableNotes(NoteEntity source) {
        List<NoteEntity> targetNotes = source.getTargetNotes();
        List<NoteEntity> allNotes = userEntity.getNotes();
        return allNotes.stream()
                .filter(i -> !targetNotes.contains(i))
                .filter(i -> !i.equals(source))
                .collect(Collectors.toList());
    }

}
