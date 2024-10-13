package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.ConversationMessage;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConversationMessageRepository
    extends CrudRepository<ConversationMessage, Integer> {
  @Query("SELECT c FROM ConversationMessage c WHERE c.conversation.id = :conversationId")
  List<ConversationMessage> findByConversationInitiator(
      @Param("conversationId") int conversationId);
}
