package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.entities.Note;
import com.odde.donut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

/** Persists a prepared authored document and refreshes the note's content-derived state. */
@Service
public class AuthoredNoteDocumentPersistence {
  private final EntityPersister entityPersister;
  private final NoteService noteService;
  private final NoteReferenceService noteReferenceService;

  public AuthoredNoteDocumentPersistence(
      EntityPersister entityPersister,
      NoteService noteService,
      NoteReferenceService noteReferenceService) {
    this.entityPersister = entityPersister;
    this.noteService = noteService;
    this.noteReferenceService = noteReferenceService;
  }

  public void persist(Note note, AuthoredNoteDocument document, Timestamp updatedAt) {
    note.setUpdatedAt(updatedAt);
    note.replaceContent(document);
    entityPersister.save(note);
    noteService.deleteOrphanImagesForPersistedContent(note);
    noteReferenceService.refreshDerivedIndexesForNote(note);
  }
}
