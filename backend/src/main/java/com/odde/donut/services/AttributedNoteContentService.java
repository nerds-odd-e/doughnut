package com.odde.donut.services;

import com.odde.donut.controllers.dto.AttributedNoteContent;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.focusContext.FocusContextConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared attributed note read for REST and MCP: authorization, content projection, and accepted
 * revision lookup inside one read transaction.
 */
@Service
public class AttributedNoteContentService {
  private final AuthorizationService authorizationService;
  private final NoteRepository noteRepository;
  private final NotebookGitBindingRepository bindingRepository;

  public AttributedNoteContentService(
      AuthorizationService authorizationService,
      NoteRepository noteRepository,
      NotebookGitBindingRepository bindingRepository) {
    this.authorizationService = authorizationService;
    this.noteRepository = noteRepository;
    this.bindingRepository = bindingRepository;
  }

  @Transactional(readOnly = true)
  public AttributedNoteContent read(User user, Integer noteId)
      throws UnexpectedNoAccessRightException {
    Note note =
        noteRepository
            .findById(noteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
    authorizationService.assertReadAuthorization(user, note);
    return new AttributedNoteContent(
        note.getId(),
        note.getTitle(),
        note.getNotebook().getName(),
        FocusContextConstants.folderPathOf(note),
        note.getContent(),
        note.getUpdatedAt(),
        bindingRepository.findAcceptedRevision(note.getNotebook().getId()));
  }
}
