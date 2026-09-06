package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import java.sql.Timestamp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WebNoteContentSaveService {
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;

  public WebNoteContentSaveService(
      NotebookGitStateLoader notebookGitStateLoader,
      NoteRepository noteRepository,
      AuthorizationService authorizationService,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public Note save(
      Integer noteId, Integer notebookId, AuthoredNoteDocument document, Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    Note note =
        notebookGitStateLoader
            .findByNotebookIdForUpdate(notebookId)
            .map(state -> findNote(state, noteId))
            .orElseGet(() -> requireNote(noteId));
    if (!notebookId.equals(note.getNotebook().getId())) {
      throw noteNotFound();
    }
    authorizationService.assertAuthorization(note);
    authoredNoteDocumentPersistence.persist(note, document, updatedAt);
    return note;
  }

  private Note findNote(NotebookGitStateLoader.LockedNotebookState state, Integer noteId) {
    return state.liveNotes().stream()
        .filter(note -> noteId.equals(note.getId()))
        .findFirst()
        .orElseThrow(this::noteNotFound);
  }

  private Note requireNote(Integer noteId) {
    return noteRepository.findById(noteId).orElseThrow(this::noteNotFound);
  }

  private ResponseStatusException noteNotFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found.");
  }
}
