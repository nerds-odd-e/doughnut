package com.odde.donut.testability.builders;

import com.odde.donut.entities.*;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;

public class ConversationBuilder extends EntityBuilder<Conversation> {
  private boolean withNoteCorrection;

  public ConversationBuilder(MakeMe makeMe) {
    super(makeMe, new Conversation());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getSubject().isEmpty()) {
      Note note = makeMe.aNote().please(needPersist);
      forANote(note);
    }
    if (this.entity.getConversationInitiator() == null) {
      entity.setConversationInitiator(makeMe.aUser().please(needPersist));
    }
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    if (withNoteCorrection) {
      makeMe
          .aConversationNoteCorrection(makeMe.aConversationMessage(entity).please(needPersist))
          .please(needPersist);
    }
  }

  public ConversationBuilder withNoteCorrection() {
    withNoteCorrection = true;
    return this;
  }

  public ConversationBuilder forANote(Note note) {
    this.entity.setNote(note);
    return this;
  }

  public ConversationBuilder from(User currentUser) {
    this.entity.setConversationInitiator(currentUser);
    return this;
  }

  public ConversationBuilder createdAt(Timestamp time) {
    entity.setCreatedAt(time);
    return this;
  }

  public ConversationBuilder forARecallPrompt(RecallPrompt recallPrompt) {
    this.entity.setRecallPrompt(recallPrompt);
    this.entity.setAccessOwnership(recallPrompt.getNotebook().getOwnership());
    return this;
  }
}
