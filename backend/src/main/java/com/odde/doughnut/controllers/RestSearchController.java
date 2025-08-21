package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
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
class RestSearchController {

  private final UserModel currentUser;
  private final NoteSearchService noteSearchService;

  public RestSearchController(UserModel currentUser, NoteSearchService noteSearchService) {
    this.currentUser = currentUser;
    this.noteSearchService = noteSearchService;
  }

  @PostMapping("/search")
  @Transactional
  public List<NoteTopology> searchForLinkTarget(@Valid @RequestBody SearchTerm searchTerm)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    return noteSearchService.searchForNotes(currentUser.getEntity(), searchTerm);
  }

  @PostMapping("/{note}/search")
  @Transactional
  public List<NoteTopology> searchForLinkTargetWithin(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody SearchTerm searchTerm)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    return noteSearchService.searchForNotesInRelationTo(currentUser.getEntity(), searchTerm, note);
  }
}
