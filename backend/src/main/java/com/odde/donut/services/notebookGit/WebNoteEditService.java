package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteReferenceService;
import com.odde.donut.services.NoteTitleNameRule;
import com.odde.donut.services.WikiLinkRewriteService;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WebNoteEditService {
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final EntityPersister entityPersister;
  private final NoteReferenceService noteReferenceService;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final NoteTitleNameRule noteTitleNameRule;

  public WebNoteEditService(
      AcceptedWebChangeService acceptedWebChangeService,
      NoteRepository noteRepository,
      AuthorizationService authorizationService,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      EntityPersister entityPersister,
      NoteReferenceService noteReferenceService,
      WikiLinkRewriteService wikiLinkRewriteService,
      NoteTitleNameRule noteTitleNameRule) {
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.entityPersister = entityPersister;
    this.noteReferenceService = noteReferenceService;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.noteTitleNameRule = noteTitleNameRule;
  }

  public Note saveTitle(
      Integer noteId, Integer notebookId, NoteUpdateTitleDTO titleDTO, Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    Set<Integer> notebookIds =
        titleDTO.getReferenceHandling() == null
            ? Set.of(notebookId)
            : noteReferenceService.notebooksToLock(
                requireNote(noteId), authorizationService.getCurrentUser(), notebookId);
    return edit(
        noteId,
        notebookId,
        notebookIds,
        note -> renameTitle(note, titleDTO, updatedAt),
        note -> "Edit note title: " + note.getTitle(),
        updatedAt);
  }

  private void renameTitle(Note note, NoteUpdateTitleDTO titleDTO, Timestamp updatedAt) {
    noteTitleNameRule.requireTitleFreeFor(
        note, note.getNotebook(), note.getFolder(), titleDTO.getNewTitle());
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

  public Note edit(
      Integer noteId,
      Integer notebookId,
      Consumer<Note> mutation,
      Function<Note, String> commitMessage,
      Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    return edit(noteId, notebookId, Set.of(notebookId), mutation, commitMessage, updatedAt);
  }

  /**
   * Like {@link #edit(Integer, Integer, Consumer, Function, Timestamp)}, committing to every
   * notebook in {@code notebookIds}; the note must still be in {@code notebookId} when the mutation
   * starts.
   */
  public Note edit(
      Integer noteId,
      Integer notebookId,
      Set<Integer> notebookIds,
      Consumer<Note> mutation,
      Function<Note, String> commitMessage,
      Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    return acceptedWebChangeService.apply(
        notebookIds,
        () -> {
          Note note = requireNote(noteId);
          if (!notebookId.equals(note.getNotebook().getId())) {
            throw noteNotFound();
          }
          authorizationService.assertAuthorization(note);
          mutation.accept(note);
          return note;
        },
        commitMessage,
        updatedAt);
  }

  Note requireNote(Integer noteId) {
    return noteRepository.findById(noteId).orElseThrow(this::noteNotFound);
  }

  private ResponseStatusException noteNotFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found.");
  }
}
