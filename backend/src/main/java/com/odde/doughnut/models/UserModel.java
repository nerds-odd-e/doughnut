package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.Getter;

import java.util.Date;
import java.util.List;

public class UserModel {

    @Getter
    private final UserEntity userEntity;
    private final ModelFactoryService modelFactoryService;

    public UserModel(UserEntity userEntity, ModelFactoryService modelFactoryService) {
        this.userEntity = userEntity;
        this.modelFactoryService = modelFactoryService;
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

    public Object getOrphanedNotes() {
        return userEntity.getOrphanedNotes();
    }

    public void assertAuthorization(NoteEntity noteEntity) throws NoAccessRightException {
        userEntity.assertAuthorization(noteEntity);
    }

    public List<NoteEntity> filterLinkableNotes(NoteEntity noteEntity, String searchTerm) {
        return userEntity.filterLinkableNotes(noteEntity, searchTerm);
    }

    public List<NoteEntity> getNewNotesToReview() {
        return userEntity.getNewNotesToReview();
    }

    public long getDailyNewNotesCount() {
        return userEntity.getDailyNewNotesCount();
    }

    public String getName() {
        return userEntity.getName();
    }

}
