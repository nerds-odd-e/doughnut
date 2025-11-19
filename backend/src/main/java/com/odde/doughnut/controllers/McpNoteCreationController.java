package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.McpNoteAddDTO;
import com.odde.doughnut.controllers.dto.NoteCreationResult;
import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.services.search.NoteSearchService;
import com.odde.doughnut.testability.TestabilitySettings;
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

  private final UserModel currentUser;
  private final WikidataService wikidataService;
  private final NoteConstructionService noteConstructionService;
  private final NoteSearchService noteSearchService;
  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;

  @Autowired
  public McpNoteCreationController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings,
      NoteSearchService noteSearchService,
      NoteRepository noteRepository,
      NoteService noteService,
      AuthorizationService authorizationService) {
    this.currentUser = currentUser;
    this.authorizationService = authorizationService;
    this.wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
    this.noteConstructionService =
        new NoteConstructionService(
            currentUser.getEntity(),
            testabilitySettings.getCurrentUTCTimestamp(),
            modelFactoryService,
            noteService);
    this.noteSearchService = noteSearchService;
    this.noteRepository = noteRepository;
  }

  @PostMapping(value = "/create")
  @Transactional
  public NoteCreationResult createNoteViaMcp(@Valid @RequestBody McpNoteAddDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    authorizationService.assertLoggedIn(currentUser.getEntity());

    var parentNoteObj = FindParentNote(currentUser.getEntity(), noteCreation);

    if (parentNoteObj.equals(new Note())) {
      throw new UnexpectedNoAccessRightException();
    }

    authorizationService.assertAuthorization(currentUser.getEntity(), parentNoteObj);

    return noteConstructionService.createNoteWithWikidataService(
        parentNoteObj,
        noteCreation.noteCreationDTO,
        currentUser.getEntity(),
        wikidataService.wrapWikidataIdWithApi(noteCreation.noteCreationDTO.wikidataId));
  }

  private Note FindParentNote(User currentUser, McpNoteAddDTO noteCreation)
      throws UnexpectedNoAccessRightException {
    SearchTerm mySearchTerm = new SearchTerm();
    mySearchTerm.setSearchKey(noteCreation.parentNote);
    mySearchTerm.setAllMyNotebooksAndSubscriptions(true);
    mySearchTerm.setAllMyCircles(true);

    List<NoteSearchResult> results = noteSearchService.searchForNotes(currentUser, mySearchTerm);

    results.sort(Comparator.comparing(NoteSearchResult::getDistance));
    if (results.isEmpty()) {
      throw new UnexpectedNoAccessRightException();
    }
    int parentId = results.get(0).getNoteTopology().getId();
    Optional<Note> parentNoteObj = noteRepository.findById(parentId);

    return parentNoteObj.orElseGet(Note::new);
  }
}
