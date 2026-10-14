package com.odde.donut.entities.repositories;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.Ownership;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends CrudRepository<Conversation, Integer> {

  Optional<Conversation>
      findFirstBySubject_RecallPromptAndConversationInitiatorAndAccessOwnershipOrderByIdAsc(
          RecallPrompt recallPrompt, User initiator, Ownership accessOwnership);

  @Query("SELECT c FROM Conversation c WHERE c.subject.recallPrompt IN :recallPrompts")
  List<Conversation> findBySubjectRecallPromptIn(
      @Param("recallPrompts") List<RecallPrompt> recallPrompts);

  String USER_CONVERSATION_CONDITION =
      "c.accessOwnership.user = :user "
          + "OR c.accessOwnership IN (SELECT o FROM Ownership o JOIN o.circle.members mem WHERE mem = :user) "
          + "OR c.conversationInitiator = :user";

  @Query(
      "SELECT c FROM Conversation c "
          + "LEFT JOIN c.conversationMessages m "
          + "WHERE "
          + USER_CONVERSATION_CONDITION
          + " "
          + "GROUP BY c "
          + "ORDER BY MAX(m.createdAt) DESC, c.createdAt DESC")
  List<Conversation> findByUserInAccessOwnershipOrConversationInitiator(
      @Param("user") User user, Pageable pageable);

  @Query(
      "SELECT cm FROM ConversationMessage cm "
          + "JOIN cm.conversation c "
          + "WHERE ("
          + USER_CONVERSATION_CONDITION
          + ") "
          + "AND (cm.sender IS NULL OR cm.sender != :user) "
          + "AND cm.readByReceiver IS NOT TRUE")
  List<ConversationMessage> findUnreadMessagesByUser(@Param("user") User user);
}
