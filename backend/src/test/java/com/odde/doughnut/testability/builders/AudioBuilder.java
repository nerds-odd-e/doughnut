package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AttachmentBlob;
import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AudioBuilder extends EntityBuilder<Audio> {
  public AudioBuilder(Audio entity, MakeMe makeMe) {
    super(makeMe, entity);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getBlob() == null) {
      AttachmentBlob audioBlob = new AttachmentBlob();
      audioBlob.setData("DEADBEEF".getBytes());
      entity.setBlob(audioBlob);
    }

    if (entity.getStorageType() == null) {
      entity.setStorageType("db");
    }

    if (entity.getName() == null) {
      entity.setName("example.mp3");
    }

    if (entity.getType() == null) {
      entity.setType("audio/mp3");
    }

    if (entity.getUser() == null) {
      entity.setUser(makeMe.aUser().please());
    }
  }

  public AudioBuilder user(User user) {
    entity.setUser(user);
    return this;
  }
}
