package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.Date;

public class NoteContentModel extends ModelForEntity<NoteEntity> {
    public NoteContentModel(NoteEntity noteEntity, ModelFactoryService modelFactoryService) {
        super(noteEntity, modelFactoryService);
    }

    public String getTitle() {
        return entity.getTitle();
    }

    public void createByUser(UserModel userModel) {
        entity.setUserEntity(userModel.getEntity());
        this.modelFactoryService.noteRepository.save(entity);
    }

    public void setAndSaveMasterReviewSetting(ReviewSettingEntity reviewSettingEntity) {
        entity.setMasterReviewSettingEntity(reviewSettingEntity);
        save();
    }
}
