package com.odde.doughnut.models;

import com.odde.doughnut.entities.ImageBlobEntity;
import com.odde.doughnut.entities.ImageEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
        MultipartFile file = entity.getUploadPictureProxy();
        if (file != null && !file.isEmpty()) {
            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setUserEntity(userModel.getEntity());
            imageEntity.setStorageType("db");
            imageEntity.setName(file.getOriginalFilename());
            imageEntity.setType(file.getContentType());
            ImageBlobEntity imageBlobEntity = getImageBlobEntity(file);
            imageEntity.setImageBlobEntity(imageBlobEntity);
            entity.setUploadPicture(imageEntity);
        }
        this.modelFactoryService.noteRepository.save(entity);
    }

    private ImageBlobEntity getImageBlobEntity(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        BufferedImage resizedImage = resizeImage(originalImage, 800, 800);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, getFileExtension(file.getOriginalFilename()), baos);
        ImageBlobEntity imageBlobEntity = new ImageBlobEntity();
        byte[] data = baos.toByteArray();
        imageBlobEntity.setData(data);
        return imageBlobEntity;
    }

    private String getFileExtension(String name) {
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf + 1);
    }

    BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
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
