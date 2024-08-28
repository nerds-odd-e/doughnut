package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.User;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ConversationRepository extends CrudRepository<Conversation, Integer> {
  List<Conversation> findByNoteCreator(User noteCreator);

  List<Conversation> findByNoteCreatorOrConversationInitiator(
      User noteCreator, User conversationInitiator);
}
