package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.ImageBlobEntity;
import com.odde.doughnut.entities.ImageEntity;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class ImageBuilder extends EntityBuilder<ImageEntity> {
    public ImageBuilder(ImageEntity entity, MakeMe makeMe) {
        super(makeMe, entity);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {
        if (entity.getImageBlobEntity() == null) {
            ImageBlobEntity imageBlobEntity = new ImageBlobEntity();
            imageBlobEntity.setData("DEADBEEF".getBytes());
            entity.setImageBlobEntity(imageBlobEntity);
        }

        if (entity.getStorageType() == null) {
            entity.setStorageType("db");
        }

        if (entity.getName() == null) {
            entity.setName("example.png");
        }

        if (entity.getType() == null) {
            entity.setType("image/png");
        }

        if (entity.getUserEntity() == null) {
            entity.setUserEntity(makeMe.aUser().please());
        }
    }
}
