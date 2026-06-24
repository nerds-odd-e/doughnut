package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAliasIndex;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteAliasIndexRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteAliasIndexService {

  @PersistenceContext private EntityManager entityManager;

  private final NoteAliasIndexRepository noteAliasIndexRepository;

  public NoteAliasIndexService(NoteAliasIndexRepository noteAliasIndexRepository) {
    this.noteAliasIndexRepository = noteAliasIndexRepository;
  }

  @Transactional
  public void refreshForNote(Note note) {
    Integer noteId = note.getId();
    entityManager.find(Note.class, noteId, LockModeType.PESSIMISTIC_WRITE);
    noteAliasIndexRepository.deleteByNoteIdInBulk(noteId);
    entityManager.flush();

    Notebook notebook = note.getNotebook();
    if (notebook == null || notebook.getId() == null) {
      return;
    }

    List<String> aliases = FrontmatterAliases.fromNoteContent(note.getContent());
    if (aliases.isEmpty()) {
      return;
    }

    Note indexOwner = entityManager.getReference(Note.class, noteId);
    Notebook notebookRef = entityManager.getReference(Notebook.class, notebook.getId());
    for (String aliasDisplay : aliases) {
      NoteAliasIndex row = new NoteAliasIndex();
      row.setNote(indexOwner);
      row.setNotebook(notebookRef);
      row.setAliasDisplay(aliasDisplay);
      row.setAliasLookupKey(FrontmatterAliases.normalizedLookupKey(aliasDisplay));
      noteAliasIndexRepository.save(row);
    }
  }
}
