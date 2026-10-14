package com.odde.donut.entities.repositories;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationNoteCorrection;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ConversationNoteCorrectionRepository
    extends CrudRepository<ConversationNoteCorrection, Integer> {
  List<ConversationNoteCorrection> findBySourceMessage_ConversationOrderByIdAsc(
      Conversation conversation);
}
