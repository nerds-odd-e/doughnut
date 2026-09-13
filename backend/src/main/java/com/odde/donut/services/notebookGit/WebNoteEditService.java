package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WebNoteEditService {
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final NotebookGitProjection projection;
  private final AcceptedSnapshotPersistence acceptedSnapshotPersistence;

  public WebNoteEditService(
      NotebookGitStateLoader notebookGitStateLoader,
      NoteRepository noteRepository,
      AuthorizationService authorizationService,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      NotebookGitProjection projection,
      AcceptedSnapshotPersistence acceptedSnapshotPersistence) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.projection = projection;
    this.acceptedSnapshotPersistence = acceptedSnapshotPersistence;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public Note saveContent(
      Integer noteId, Integer notebookId, AuthoredNoteDocument document, Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    return edit(
        noteId,
        notebookId,
        note -> authoredNoteDocumentPersistence.persist(note, document, updatedAt),
        note -> "Edit note content: " + note.getTitle(),
        updatedAt);
  }

  private Note edit(
      Integer noteId,
      Integer notebookId,
      Consumer<Note> mutation,
      Function<Note, String> commitMessage,
      Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    var lockedState = notebookGitStateLoader.findByNotebookIdForUpdate(notebookId);
    Note note =
        lockedState.map(state -> findNote(state, noteId)).orElseGet(() -> requireNote(noteId));
    if (!notebookId.equals(note.getNotebook().getId())) {
      throw noteNotFound();
    }
    authorizationService.assertAuthorization(note);
    if (lockedState.isEmpty()) {
      mutation.accept(note);
      return note;
    }

    NotebookGitStateLoader.LockedNotebookState state = lockedState.orElseThrow();
    NotebookGitBinding binding = state.binding();
    ObjectId persistedAcceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    try (NotebookGitBundleImporter.ImportedBundle accepted =
        NotebookGitBundleImporter.importMainHead(binding.getBundleBytes(), "accepted-bundle")) {
      if (!accepted.mainHead().equals(persistedAcceptedHead)) {
        throw new IllegalStateException("Accepted bundle main does not match its persisted head");
      }
      boolean acceptedTreeMatchedBeforeSave = acceptedTreeMatches(state, accepted);
      mutation.accept(note);
      if (!acceptedTreeMatchedBeforeSave) {
        return note;
      }
      if (acceptedTreeMatches(state, accepted)) {
        return note;
      }

      List<PortableTreeEntry> entries =
          PortableTreeSnapshot.build(
              state.notebook().getReadmeContent(),
              state.folders(),
              NotebookExportRows.notes(state.liveNotes()));
      acceptedSnapshotPersistence.persist(
          accepted, entries, binding, updatedAt, commitMessage.apply(note));
    }
    return note;
  }

  private boolean acceptedTreeMatches(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitBundleImporter.ImportedBundle accepted) {
    return projection.matchesAcceptedTree(
        state.notebook(),
        state.folders(),
        state.liveNotes(),
        accepted.repository(),
        accepted.mainHead());
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
