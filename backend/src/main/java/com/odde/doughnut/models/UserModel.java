package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserModel extends ModelForEntity<UserEntity>{
    public UserModel(UserEntity userEntity, ModelFactoryService modelFactoryService) {
        super(userEntity, modelFactoryService);
    }

    public String getName() {
        return entity.getName();
    }

    public int getDailyNewNotesCount() {
        return entity.getDailyNewNotesCount();
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

    public List<NoteEntity> getNewNotesToReview(Timestamp currentTime) {
        List<NoteEntity> notes = new ArrayList<>();
        int countDown = getNewNotesCountForToday(currentTime);
        for(NoteEntity note: entity.getNotes()) {
            if (countDown <= 0) {
                break;
            }
            boolean byNoteEntity = entity.getReviewPoints().stream().anyMatch(rp->rp.getNoteEntity() == note);
            if (!byNoteEntity) {
                notes.add(note);
                countDown--;
            }
        }

        return notes;
    }

    private int getNewNotesCountForToday(Timestamp currentTime) {
        int countDown = entity.getDailyNewNotesCount();
        countDown -= entity.getReviewPoints().stream().filter(p -> sameDay(p.getLastReviewedAt(), currentTime)).count();

        return countDown;
    }

    private boolean sameDay(Timestamp lastReviewedAt, Timestamp currentTime) {
        return lastReviewedAt == currentTime;
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
