package com.odde.donut.testability.builders;

import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.User;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.UUID;

public class ConversationNoteCorrectionBuilder extends EntityBuilder<ConversationNoteCorrection> {
  public ConversationNoteCorrectionBuilder(ConversationMessage sourceMessage, MakeMe makeMe) {
    super(makeMe, new ConversationNoteCorrection());
    entity.setSourceMessage(sourceMessage);
    entity.setNote(sourceMessage.getConversation().getSubjectNote());
    entity.setResponseId(UUID.randomUUID().toString());
    entity.setToolCallId("call_1");
    entity.setField(ConversationNoteCorrection.Field.CONTENT);
    entity.setProposedValue("Reviewed clarification");
    entity.setReason("Makes the distinction explicit");
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public ConversationNoteCorrectionBuilder baseRevision(String revision) {
    entity.setBaseRevision(revision);
    return this;
  }

  public ConversationNoteCorrectionBuilder acceptedBy(User reviewer) {
    entity.setDecision(ConversationNoteCorrection.Decision.ACCEPTED);
    entity.setDecidedBy(reviewer);
    entity.setDecidedAt(new Timestamp(System.currentTimeMillis()));
    entity.setReviewedValue(entity.getProposedValue());
    entity.setAcceptedRevision("a".repeat(40));
    return this;
  }
}
