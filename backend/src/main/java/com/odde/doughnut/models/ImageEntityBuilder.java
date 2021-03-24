package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.ImageUtils;
import com.odde.doughnut.entities.ImageBlobEntity;
import com.odde.doughnut.entities.ImageEntity;
import org.springframework.core.io.InputStreamSource;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageEntityBuilder {
    private final ImageUtils imageUtils = new ImageUtils();

    public ImageEntityBuilder() {
    }

    ImageEntity buildImageEntityFromUploadedPicture(UserModel userModel, MultipartFile file) throws IOException {
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setUserEntity(userModel.getEntity());
        imageEntity.setStorageType("db");
        imageEntity.setName(file.getOriginalFilename());
        imageEntity.setType(file.getContentType());
        ImageBlobEntity imageBlobEntity = getImageBlobEntity(file);
        imageEntity.setImageBlobEntity(imageBlobEntity);
        return imageEntity;
    }

    ImageBlobEntity getImageBlobEntity(MultipartFile file) throws IOException {
        byte[] data = imageUtils.toResizedImageByteArray(file, file.getOriginalFilename());
        ImageBlobEntity imageBlobEntity = new ImageBlobEntity();
        imageBlobEntity.setData(data);
        return imageBlobEntity;
    }
}