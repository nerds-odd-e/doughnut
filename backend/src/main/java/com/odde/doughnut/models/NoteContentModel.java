package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.Date;

public class NoteContentModel extends ModelForEntity<NoteEntity> {
    public NoteContentModel(NoteEntity noteEntity, ModelFactoryService modelFactoryService) {
        super(noteEntity, modelFactoryService);
    }

    public String getTitle() {
        return entity.getTitle();
    }

    public void linkNote(NoteEntity targetNote) {
        entity.linkToNote(targetNote);
        entity.setUpdatedDatetime(new Date());
        modelFactoryService.noteRepository.save(entity);
    }

    public void createForUser(UserModel userModel) {
        setOwnership(userModel);
        this.modelFactoryService.noteRepository.save(entity);
    }

    public void setOwnership(UserModel userModel) {
        entity.setUserEntity(userModel.getEntity());
        entity.setOwnershipEntity(userModel.getEntity().getOwnershipEntity());
    }
}
