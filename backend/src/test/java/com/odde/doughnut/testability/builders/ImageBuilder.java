package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.ImageBlob;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class ImageBuilder extends EntityBuilder<Image> {
  public ImageBuilder(Image entity, MakeMe makeMe) {
    super(makeMe, entity);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getImageBlob() == null) {
      ImageBlob imageBlob = new ImageBlob();
      imageBlob.setData("DEADBEEF".getBytes());
      entity.setImageBlob(imageBlob);
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

    if (entity.getUser() == null) {
      entity.setUser(makeMe.aUser().please());
    }
  }
}
