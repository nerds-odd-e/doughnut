package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteReferenceService;
import com.odde.donut.services.WikiLinkRewriteService;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
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
  private final EntityPersister entityPersister;
  private final NoteReferenceService noteReferenceService;
  private final WikiLinkRewriteService wikiLinkRewriteService;

  public WebNoteEditService(
      NotebookGitStateLoader notebookGitStateLoader,
      NoteRepository noteRepository,
      AuthorizationService authorizationService,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      NotebookGitProjection projection,
      AcceptedSnapshotPersistence acceptedSnapshotPersistence,
      EntityPersister entityPersister,
      NoteReferenceService noteReferenceService,
      WikiLinkRewriteService wikiLinkRewriteService) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.projection = projection;
    this.acceptedSnapshotPersistence = acceptedSnapshotPersistence;
    this.entityPersister = entityPersister;
    this.noteReferenceService = noteReferenceService;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public Note saveTitle(
      Integer noteId, Integer notebookId, NoteUpdateTitleDTO titleDTO, Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    return edit(
        noteId,
        notebookId,
        note -> renameTitle(note, titleDTO, updatedAt),
        note -> "Edit note title: " + note.getTitle(),
        updatedAt);
  }

  private void renameTitle(Note note, NoteUpdateTitleDTO titleDTO, Timestamp updatedAt) {
    User viewer = authorizationService.getCurrentUser();
    assertReferencedTitleRenameIsUnambiguous(note, titleDTO, viewer);
    boolean titleChanged = !Objects.equals(note.getTitle(), titleDTO.getNewTitle());
    if (titleChanged && titleDTO.getReferenceHandling() != null) {
      wikiLinkRewriteService.rewriteInboundWikiLinksForTitleRename(
          note, titleDTO.getNewTitle(), updatedAt, viewer, titleDTO.getReferenceHandling());
      return;
    }
    note.setUpdatedAt(updatedAt);
    note.setTitle(new DisplayName(titleDTO.getNewTitle()));
    entityPersister.save(note);
  }

  private void assertReferencedTitleRenameIsUnambiguous(
      Note note, NoteUpdateTitleDTO titleDTO, User viewer) {
    if (Objects.equals(note.getTitle(), titleDTO.getNewTitle())
        || !noteReferenceService.isReferencedForViewer(note, viewer)
        || titleDTO.getReferenceHandling() != null) {
      return;
    }
    String message =
        "This note is linked from other notes. Choose how wiki references should be updated"
            + " when renaming.";
    ApiError apiError = new ApiError(message, ApiError.ErrorType.BINDING_ERROR);
    apiError.add("referenceHandling", message);
    throw new ApiException(apiError);
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

  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public Note edit(
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
