package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.ImageUtils;
import com.odde.doughnut.entities.ImageBlob;
import com.odde.doughnut.entities.ImageEntity;
import com.odde.doughnut.entities.UserEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class ImageEntityBuilder {
    private final ImageUtils imageUtils = new ImageUtils();

    public ImageEntityBuilder() {
    }

    public ImageEntity buildImageEntityFromUploadedPicture(UserEntity userEntity, MultipartFile file) throws IOException {
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setUserEntity(userEntity);
        imageEntity.setStorageType("db");
        imageEntity.setName(file.getOriginalFilename());
        imageEntity.setType(file.getContentType());
        ImageBlob imageBlob = getImageBlob(file);
        imageEntity.setImageBlob(imageBlob);
        return imageEntity;
    }

    ImageBlob getImageBlob(MultipartFile file) throws IOException {
        byte[] data = imageUtils.toResizedImageByteArray(file, file.getOriginalFilename());
        ImageBlob imageBlob = new ImageBlob();
        imageBlob.setData(data);
        return imageBlob;
    }
}