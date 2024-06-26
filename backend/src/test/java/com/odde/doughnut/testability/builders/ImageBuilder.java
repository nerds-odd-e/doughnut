package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AttachmentBlob;
import com.odde.doughnut.entities.Image;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class ImageBuilder extends EntityBuilder<Image> {
  public ImageBuilder(Image entity, MakeMe makeMe) {
    super(makeMe, entity);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getBlob() == null) {
      AttachmentBlob attachmentBlob = new AttachmentBlob();
      attachmentBlob.setData("DEADBEEF".getBytes());
      entity.setBlob(attachmentBlob);
    }

    if (entity.getName() == null) {
      entity.setName("example.png");
    }

    if (entity.getContentType() == null) {
      entity.setContentType("image/png");
    }

    if (entity.getUser() == null) {
      entity.setUser(makeMe.aUser().please());
    }
  }
}
