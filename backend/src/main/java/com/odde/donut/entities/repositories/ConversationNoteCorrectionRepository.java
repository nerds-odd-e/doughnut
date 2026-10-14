package com.odde.donut.entities.repositories;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationNoteCorrection;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConversationNoteCorrectionRepository
    extends CrudRepository<ConversationNoteCorrection, Integer> {
  List<ConversationNoteCorrection> findBySourceMessage_ConversationOrderByIdAsc(
      Conversation conversation);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT correction FROM ConversationNoteCorrection correction WHERE correction.id = :id")
  Optional<ConversationNoteCorrection> findByIdForUpdate(@Param("id") Integer id);
}
