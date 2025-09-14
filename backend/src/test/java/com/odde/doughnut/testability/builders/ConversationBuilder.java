package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class ConversationBuilder extends EntityBuilder<Conversation> {
  public ConversationBuilder(MakeMe makeMe) {
    super(makeMe, new Conversation());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getSubject().isEmpty()) {
      User assessmentOwner = makeMe.aUser().please(needPersist);
      AssessmentQuestionInstance instance =
          makeMe
              .anAssessmentAttempt(assessmentOwner)
              .withOneQuestion()
              .please(needPersist)
              .getAssessmentQuestionInstances()
              .get(0);
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

  public ConversationBuilder forANote(Note note) {
    this.entity.setNote(note);
    return this;
  }

  public ConversationBuilder from(UserModel currentUser) {
    return from(currentUser.getEntity());
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
    return this;
  }
}
