package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class ConversationBuilder extends EntityBuilder<Conversation> {
  public ConversationBuilder(MakeMe makeMe) {
    super(makeMe, new Conversation());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getAssessmentQuestionInstance() == null) {
      User assessmentOwner = makeMe.aUser().please(needPersist);
      AssessmentQuestionInstance instance =
          makeMe
              .anAssessmentAttempt(assessmentOwner)
              .withOneQuestion()
              .please(needPersist)
              .getAssessmentQuestionInstances()
              .getFirst();
      forAnAssessmentQuestionInstance(instance);
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
}
