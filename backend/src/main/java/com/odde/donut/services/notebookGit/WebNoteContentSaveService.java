package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
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
  private final NotebookGitProjection projection;
  private final EntityPersister entityPersister;

  public WebNoteContentSaveService(
      NotebookGitStateLoader notebookGitStateLoader,
      NoteRepository noteRepository,
      AuthorizationService authorizationService,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      NotebookGitProjection projection,
      EntityPersister entityPersister) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.projection = projection;
    this.entityPersister = entityPersister;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public Note save(
      Integer noteId, Integer notebookId, AuthoredNoteDocument document, Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    var lockedState = notebookGitStateLoader.findByNotebookIdForUpdate(notebookId);
    Note note =
        lockedState.map(state -> findNote(state, noteId)).orElseGet(() -> requireNote(noteId));
    if (!notebookId.equals(note.getNotebook().getId())) {
      throw noteNotFound();
    }
    authorizationService.assertAuthorization(note);
    if (lockedState.isEmpty() || note.getFolder() != null) {
      authoredNoteDocumentPersistence.persist(note, document, updatedAt);
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
      boolean acceptedTreeMatches =
          projection.matchesAcceptedTree(
              state.notebook(),
              state.folders(),
              state.liveNotes(),
              accepted.repository(),
              accepted.mainHead());
      authoredNoteDocumentPersistence.persist(note, document, updatedAt);
      if (!acceptedTreeMatches) {
        return note;
      }

      List<PortableTreeEntry> entries =
          PortableTreeSnapshot.build(
              state.notebook().getReadmeContent(),
              state.folders(),
              NotebookExportRows.notes(state.liveNotes()));
      NotebookGitBundleBuilder.append(
          accepted.repository(),
          accepted.mainHead(),
          entries,
          NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
          NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
          "Edit note content: " + note.getTitle(),
          updatedAt.toInstant());
      NotebookGitBundleWriter.BundleWriteResult written =
          NotebookGitBundleWriter.write(accepted.repository());
      binding.setAcceptedGitObjectId(written.headObjectId());
      binding.setBundleBytes(written.bundleBytes());
      binding.setUpdatedAt(updatedAt);
      entityPersister.save(binding);
    }
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
