package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.AssimilationSequenceSkip;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface AssimilationSequenceSkipRepository
    extends CrudRepository<AssimilationSequenceSkip, Integer> {
  Optional<AssimilationSequenceSkip> findByUserAndNoteAndPropertyKey(
      User user, Note note, String propertyKey);
}
