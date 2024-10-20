package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import org.apache.logging.log4j.util.Strings;

public class ConversationMessageBuilder extends EntityBuilder<ConversationMessage> {
  public ConversationMessageBuilder(MakeMe makeMe) {
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
    entity.setReadByReceiver(false);
  }

  public ConversationMessageBuilder forConversation(Conversation conversation) {
    this.entity.setConversation(conversation);
    conversation.getConversationMessages().add(this.entity);
    return this;
  }
}
