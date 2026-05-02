package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.McpNoteAddDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.RelationshipLiteralSearchHit;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.services.search.NoteSearchService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/mcp/notes")
public class McpNoteCreationController {

  private final WikidataService wikidataService;
  private final NoteConstructionService noteConstructionService;
  private final NoteSearchService noteSearchService;
  private final NoteService noteService;
  private final AuthorizationService authorizationService;

  @Autowired
  public McpNoteCreationController(
      WikidataService wikidataService,
      NoteConstructionService noteConstructionService,
      NoteSearchService noteSearchService,
      NoteService noteService,
      AuthorizationService authorizationService) {
    this.wikidataService = wikidataService;
    this.noteConstructionService = noteConstructionService;
    this.noteSearchService = noteSearchService;
    this.noteService = noteService;
    this.authorizationService = authorizationService;
  }

  @PostMapping(value = "/create")
  @Transactional
  public NoteRealm createNoteViaMcp(@Valid @RequestBody McpNoteAddDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    authorizationService.assertLoggedIn();

    var parentNoteObj = FindParentNote(authorizationService.getCurrentUser(), noteCreation);

    if (parentNoteObj.equals(new Note())) {
      throw new UnexpectedNoAccessRightException();
    }

    authorizationService.assertAuthorization(parentNoteObj);

    return noteConstructionService.createNoteWithWikidataService(
        parentNoteObj,
        noteCreation.noteCreationDTO,
        authorizationService.getCurrentUser(),
        wikidataService.wrapWikidataIdWithApi(noteCreation.noteCreationDTO.wikidataId));
  }

  private Note FindParentNote(User currentUser, McpNoteAddDTO noteCreation)
      throws UnexpectedNoAccessRightException {
    SearchTerm mySearchTerm = new SearchTerm();
    mySearchTerm.setSearchKey(noteCreation.parentNote);
    mySearchTerm.setAllMyNotebooksAndSubscriptions(true);
    mySearchTerm.setAllMyCircles(true);

    List<RelationshipLiteralSearchHit> results =
        noteSearchService.searchForNotes(currentUser, mySearchTerm);

    List<NoteSearchResult> noteOnly =
        results.stream()
            .filter(RelationshipLiteralSearchHit::isNote)
            .map(RelationshipLiteralSearchHit::getNoteSearchResult)
            .sorted(Comparator.comparing(NoteSearchResult::getDistance))
            .toList();
    if (noteOnly.isEmpty()) {
      throw new UnexpectedNoAccessRightException();
    }
    int parentId = noteOnly.get(0).getNoteTopology().getId();
    Optional<Note> parentNoteObj = noteService.findById(parentId);

    return parentNoteObj.orElseGet(Note::new);
  }
}
