package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import org.apache.logging.log4j.util.Strings;

public class ConversationBuilder extends EntityBuilder<Conversation> {
  public ConversationBuilder(MakeMe makeMe) {
    super(makeMe, new Conversation());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getAssessmentQuestionInstance() == null) {
      throw new RuntimeException("AssessmentQuestionInstance is required");
    }
    if (Strings.isBlank(this.entity.getMessage())) {
      entity.setMessage("This is a feedback");
    }
    if (this.entity.getConversationInitiator() == null) {
      entity.setConversationInitiator(makeMe.aUser().please(needPersist));
    }
  }

  public ConversationBuilder forAnAssessmentQuestionInstance(
      AssessmentQuestionInstance assessmentQuestionInstance) {
    this.entity.setAssessmentQuestionInstance(assessmentQuestionInstance);
    return this;
  }

  public ConversationBuilder from(UserModel currentUser) {
    this.entity.setConversationInitiator(currentUser.getEntity());
    return this;
  }

  public ConversationBuilder messagge(String msg) {
    this.entity.setMessage(msg);
    return this;
  }
}
