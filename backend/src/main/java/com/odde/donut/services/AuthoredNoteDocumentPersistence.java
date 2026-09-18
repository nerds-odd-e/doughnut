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
    // `save` merges an already-existing note rather than mutating `note` in place, so the
    // returned, managed instance is the one later steps must operate on. Using the original
    // reference here throws EntityExistsException from a still-detached note (e.g. one loaded
    // via a Spring MVC @PathVariable converter outside this method's transaction).
    Note managed = entityPersister.save(note);
    noteService.deleteOrphanImagesForPersistedContent(managed);
    noteReferenceService.refreshDerivedIndexesForNote(managed);
  }
}
