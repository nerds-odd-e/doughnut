package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserAssistantThread;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class UserAssistantThreadBuilder extends EntityBuilder<UserAssistantThread> {
  Note note;
  User user;

  public UserAssistantThreadBuilder(MakeMe makeMe, String threadId) {
    super(makeMe, new UserAssistantThread());
    entity.setThreadId(threadId);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (note == null) {
      note = makeMe.aNote().please(needPersist);
    }
    entity.setNote(note);
    if (user == null) {
      user = makeMe.aUser().please(needPersist);
    }
    entity.setUser(user);
  }

  public UserAssistantThreadBuilder forNote(Note note) {
    this.note = note;
    return this;
  }

  public UserAssistantThreadBuilder by(UserModel currentUser) {
    this.user = currentUser.getEntity();
    return this;
  }
}
