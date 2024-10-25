package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.User;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends CrudRepository<Conversation, Integer> {

  String USER_CONVERSATION_CONDITION =
      "c.subjectOwnership.user = :user "
          + "OR c.subjectOwnership IN (SELECT o FROM Ownership o JOIN o.circle.members mem WHERE mem = :user) "
          + "OR c.conversationInitiator = :user";

  @Query(
      "SELECT c FROM Conversation c "
          + "LEFT JOIN c.conversationMessages m "
          + "WHERE "
          + USER_CONVERSATION_CONDITION
          + " "
          + "GROUP BY c "
          + "ORDER BY MAX(m.createdAt) DESC, c.createdAt DESC")
  List<Conversation> findByUserInSubjectOwnershipOrConversationInitiator(@Param("user") User user);

  @Query(
      "SELECT cm FROM ConversationMessage cm "
          + "JOIN cm.conversation c "
          + "WHERE ("
          + USER_CONVERSATION_CONDITION
          + ") "
          + "AND cm.sender != :user "
          + "AND cm.readByReceiver IS NOT TRUE")
  List<ConversationMessage> findUnreadMessagesByUser(@Param("user") User user);
}
