package com.odde.donut.testability.builders;

import com.odde.donut.entities.AttachmentBlob;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;

public class ImageBuilder extends EntityBuilder<Image> {
  public ImageBuilder(Image entity, MakeMe makeMe) {
    super(makeMe, entity);
  }

  public ImageBuilder forNote(Note note) {
    entity.setNote(note);
    return this;
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
