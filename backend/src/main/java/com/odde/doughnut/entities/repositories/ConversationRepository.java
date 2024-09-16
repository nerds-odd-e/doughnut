package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.User;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends CrudRepository<Conversation, Integer> {
  @Query(
      "SELECT c FROM Conversation c WHERE c.subjectOwnership.user = :user OR c.subjectOwnership IN (SELECT o FROM Ownership o JOIN o.circle.members mem WHERE mem = :user) OR c.conversationInitiator = :user")
  List<Conversation> findByUserInSubjectOwnershipOrConversationInitiator(@Param("user") User user);
}
