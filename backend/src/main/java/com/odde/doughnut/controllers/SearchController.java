package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.search.NoteSearchService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class SearchController {

  private final NoteSearchService noteSearchService;
  private final AuthorizationService authorizationService;

  public SearchController(
      NoteSearchService noteSearchService, AuthorizationService authorizationService) {
    this.noteSearchService = noteSearchService;
    this.authorizationService = authorizationService;
  }

  @PostMapping("/search")
  @Transactional
  public List<NoteSearchResult> searchForLinkTarget(@Valid @RequestBody SearchTerm searchTerm)
      throws UnexpectedNoAccessRightException {
    if (searchTerm == null) {
      throw new IllegalArgumentException("SearchTerm cannot be null");
    }
    authorizationService.assertLoggedIn();
    return noteSearchService.searchForNotes(authorizationService.getCurrentUser(), searchTerm);
  }

  @PostMapping("/{note}/search")
  @Transactional
  public List<NoteSearchResult> searchForLinkTargetWithin(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody SearchTerm searchTerm)
      throws UnexpectedNoAccessRightException {
    if (searchTerm == null) {
      throw new IllegalArgumentException("SearchTerm cannot be null");
    }
    authorizationService.assertLoggedIn();
    return noteSearchService.searchForNotesInRelationTo(
        authorizationService.getCurrentUser(), searchTerm, note);
  }

  @PostMapping("/semantic-search")
  @Transactional
  public List<NoteSearchResult> semanticSearch(@Valid @RequestBody SearchTerm searchTerm)
      throws UnexpectedNoAccessRightException {
    if (searchTerm == null) {
      throw new IllegalArgumentException("SearchTerm cannot be null");
    }
    authorizationService.assertLoggedIn();
    return noteSearchService.semanticSearchForNotes(
        authorizationService.getCurrentUser(), searchTerm);
  }

  @PostMapping("/{note}/semantic-search")
  @Transactional
  public List<NoteSearchResult> semanticSearchWithin(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody SearchTerm searchTerm)
      throws UnexpectedNoAccessRightException {
    if (searchTerm == null) {
      throw new IllegalArgumentException("SearchTerm cannot be null");
    }
    authorizationService.assertLoggedIn();
    return noteSearchService.semanticSearchForNotesInRelationTo(
        authorizationService.getCurrentUser(), searchTerm, note);
  }
}
