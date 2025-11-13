package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import org.apache.logging.log4j.util.Strings;

public class ConversationMessageBuilder extends EntityBuilder<ConversationMessage> {
  public ConversationMessageBuilder(Conversation conversation, MakeMe makeMe) {
    super(makeMe, new ConversationMessage());
    this.entity.setConversation(conversation);
    conversation.getConversationMessages().add(this.entity);
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

  public ConversationMessageBuilder sender(User user) {
    entity.setSender(user);
    return this;
  }

  public ConversationMessageBuilder readByReceiver() {
    entity.setReadByReceiver(true);
    return this;
  }

  public ConversationMessageBuilder createdAt(Timestamp time) {
    entity.setCreatedAt(time);
    return this;
  }

  public ConversationMessageBuilder message(String message) {
    entity.setMessage(message);
    return this;
  }
}
