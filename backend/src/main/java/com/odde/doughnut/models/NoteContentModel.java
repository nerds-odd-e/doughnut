package com.odde.doughnut.models;

import com.odde.doughnut.entities.ImageEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class NoteContentModel extends ModelForEntity<NoteEntity> {

    public NoteContentModel(NoteEntity noteEntity, ModelFactoryService modelFactoryService) {
        super(noteEntity, modelFactoryService);
    }

    public String getTitle() {
        return entity.getTitle();
    }

    public void createByUser(UserModel userModel) throws IOException {
        entity.setUserEntity(userModel.getEntity());
        fetchUploadedPicture(userModel);
        modelFactoryService.noteRepository.save(entity);
    }

    private void fetchUploadedPicture(UserModel userModel) throws IOException {
        MultipartFile file = entity.getUploadPictureProxy();
        if (file != null && !file.isEmpty()) {
            ImageEntity imageEntity = new ImageEntityBuilder().buildImageEntityFromUploadedPicture(userModel, file);
            entity.setUploadPicture(imageEntity);
        }
    }

    public void update(UserModel userModel) throws IOException {
        fetchUploadedPicture(userModel);
        modelFactoryService.noteRepository.save(entity);
    }

    public void setAndSaveMasterReviewSetting(ReviewSettingEntity reviewSettingEntity) {
        ReviewSettingEntity current = entity.getMasterReviewSettingEntity();
        if(current == null) {
            entity.setMasterReviewSettingEntity(reviewSettingEntity);
        }
        else {
            BeanUtils.copyProperties(reviewSettingEntity, entity.getMasterReviewSettingEntity());
        }
        save();
    }

}
