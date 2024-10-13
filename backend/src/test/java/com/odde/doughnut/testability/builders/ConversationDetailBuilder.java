package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import org.apache.logging.log4j.util.Strings;

public class ConversationDetailBuilder extends EntityBuilder<ConversationMessage> {
  public ConversationDetailBuilder(MakeMe makeMe) {
    super(makeMe, new ConversationMessage());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getConversation() == null) {
      throw new RuntimeException("Conversation is required");
    }
    if (Strings.isBlank(this.entity.getMessage())) {
      entity.setMessage("This is a feedback");
    }
  }

  public ConversationDetailBuilder forConversationInstance(Conversation conversation) {
    this.entity.setConversation(conversation);
    return this;
  }
}
