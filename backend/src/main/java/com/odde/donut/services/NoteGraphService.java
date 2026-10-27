package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.focusContext.FocusContextResult;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.focusContext.RetrievalConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Graph focus-context read for MCP: authorization and {@link FocusContextRetrievalService#retrieve}
 * with {@link RetrievalConfig#forGraphApi} inside one read transaction.
 */
@Service
public class NoteGraphService {
  private final AuthorizationService authorizationService;
  private final NoteRepository noteRepository;
  private final FocusContextRetrievalService focusContextRetrievalService;

  public NoteGraphService(
      AuthorizationService authorizationService,
      NoteRepository noteRepository,
      FocusContextRetrievalService focusContextRetrievalService) {
    this.authorizationService = authorizationService;
    this.noteRepository = noteRepository;
    this.focusContextRetrievalService = focusContextRetrievalService;
  }

  @Transactional(readOnly = true)
  public FocusContextResult getGraph(User user, Integer noteId, int tokenLimit)
      throws UnexpectedNoAccessRightException {
    Note note =
        noteRepository
            .findById(noteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
    authorizationService.assertReadAuthorization(user, note);
    return focusContextRetrievalService.retrieve(
        note, user, RetrievalConfig.forGraphApi(tokenLimit));
  }
}
