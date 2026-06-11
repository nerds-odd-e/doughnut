package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.PropertyKeyNaming;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotePropertyIndex;
import com.odde.doughnut.entities.repositories.NotePropertyIndexRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotePropertyIndexService {

  @PersistenceContext private EntityManager entityManager;

  private final NotePropertyIndexRepository notePropertyIndexRepository;

  public NotePropertyIndexService(NotePropertyIndexRepository notePropertyIndexRepository) {
    this.notePropertyIndexRepository = notePropertyIndexRepository;
  }

  @Transactional
  public void refreshForNote(Note note) {
    Integer noteId = note.getId();
    entityManager.find(Note.class, noteId, LockModeType.PESSIMISTIC_WRITE);
    notePropertyIndexRepository.deleteByNoteIdInBulk(noteId);
    entityManager.flush();
    Note indexOwner = entityManager.getReference(Note.class, noteId);
    NoteContentMarkdown.splitLeadingFrontmatter(note.getContent() == null ? "" : note.getContent())
        .ifPresent(
            lf ->
                lf.frontmatter()
                    .keys()
                    .forEach(
                        key -> {
                          if (!PropertyKeyNaming.isReservedStructuralKey(key)) {
                            NotePropertyIndex row = new NotePropertyIndex();
                            row.setNote(indexOwner);
                            row.setPropertyKey(key);
                            notePropertyIndexRepository.save(row);
                          }
                        }));
  }
}
