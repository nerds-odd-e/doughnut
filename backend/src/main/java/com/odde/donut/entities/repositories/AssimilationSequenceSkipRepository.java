package com.odde.donut.entities.repositories;

import com.odde.donut.entities.AssimilationSequenceSkip;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface AssimilationSequenceSkipRepository
    extends CrudRepository<AssimilationSequenceSkip, Integer> {
  Optional<AssimilationSequenceSkip> findByUserAndNoteAndPropertyKey(
      User user, Note note, String propertyKey);

  List<AssimilationSequenceSkip> findByUserAndNote(User user, Note note);
}
