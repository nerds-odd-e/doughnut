package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class UserModel extends ModelForEntity<UserEntity>{
    public UserModel(UserEntity userEntity, ModelFactoryService modelFactoryService) {
        super(userEntity, modelFactoryService);
    }

    public String getName() {
        return entity.getName();
    }

    public long getDailyNewNotesCount() {
        return entity.getDailyNewNotesCount();
    }

    public void setDailyNewNotesCount(Integer dailyNewNotesCount) {
        entity.setDailyNewNotesCount(dailyNewNotesCount);
        save();
    }

    public void setSpaceIntervals(String spaceIntervals) {
        entity.setSpaceIntervals(spaceIntervals);
        save();
    }

    private void save() {
        modelFactoryService.userRepository.save(entity);
    }

    public List<NoteEntity> getOrphanedNotes() {
        return entity.getOrphanedNotes();
    }

    public void assertAuthorization(NoteEntity noteEntity) throws NoAccessRightException {
        if (! entity.owns(noteEntity)) {
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
        List<NoteEntity> notes = entity.getNotes();
        notes.sort(Comparator.comparing(NoteEntity::getUpdatedDatetime).reversed());
        return notes;
    }

    private List<NoteEntity> getAllLinkableNotes(NoteEntity source) {
        List<NoteEntity> targetNotes = source.getTargetNotes();
        List<NoteEntity> allNotes = entity.getNotes();
        return allNotes.stream()
                .filter(i -> !targetNotes.contains(i))
                .filter(i -> !i.equals(source))
                .collect(Collectors.toList());
    }

}
