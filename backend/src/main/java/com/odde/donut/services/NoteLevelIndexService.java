package com.odde.donut.services;

import com.odde.donut.algorithms.FrontmatterNoteLevel;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteLevelIndex;
import com.odde.donut.entities.repositories.NoteLevelIndexRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteLevelIndexService {

  @PersistenceContext private EntityManager entityManager;

  private final NoteLevelIndexRepository noteLevelIndexRepository;

  public NoteLevelIndexService(NoteLevelIndexRepository noteLevelIndexRepository) {
    this.noteLevelIndexRepository = noteLevelIndexRepository;
  }

  @Transactional
  public void refreshForNote(Note note) {
    Integer noteId = note.getId();
    entityManager.find(Note.class, noteId, LockModeType.PESSIMISTIC_WRITE);

    Optional<Integer> level = FrontmatterNoteLevel.fromNoteContent(note.getContent());
    NoteLevelIndex existing = entityManager.find(NoteLevelIndex.class, noteId);
    if (level.isEmpty()) {
      if (existing != null) {
        noteLevelIndexRepository.delete(existing);
      }
      return;
    }

    if (existing != null) {
      existing.setLevel(level.get());
      return;
    }

    Note indexOwner = entityManager.getReference(Note.class, noteId);
    NoteLevelIndex row = new NoteLevelIndex();
    row.setNote(indexOwner);
    row.setLevel(level.get());
    noteLevelIndexRepository.save(row);
  }
}
