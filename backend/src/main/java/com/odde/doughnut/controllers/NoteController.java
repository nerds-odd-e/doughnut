package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.DuplicateWikidataIdException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GraphRAGService;
import com.odde.doughnut.services.NoteRealmService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class NoteController {

  private final EntityPersister entityPersister;
  private final WikidataService wikidataService;
  private final NoteService noteService;
  private final AuthorizationService authorizationService;
  private final UserService userService;
  private final GraphRAGService graphRAGService;
  private final TestabilitySettings testabilitySettings;
  private final NoteRealmService noteRealmService;

  public NoteController(
      EntityPersister entityPersister,
      WikidataService wikidataService,
      NoteService noteService,
      AuthorizationService authorizationService,
      UserService userService,
      GraphRAGService graphRAGService,
      TestabilitySettings testabilitySettings,
      NoteRealmService noteRealmService) {
    this.entityPersister = entityPersister;
    this.wikidataService = wikidataService;
    this.noteService = noteService;
    this.authorizationService = authorizationService;
    this.userService = userService;
    this.graphRAGService = graphRAGService;
    this.testabilitySettings = testabilitySettings;
    this.noteRealmService = noteRealmService;
  }

  @PostMapping(value = "/{note}/updateWikidataId")
  @Transactional
  public NoteRealm updateWikidataId(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @RequestBody WikidataAssociationCreation wikidataAssociationCreation)
      throws BindException, UnexpectedNoAccessRightException, IOException, InterruptedException {
    authorizationService.assertAuthorization(note);
    WikidataIdWithApi wikidataIdWithApi =
        wikidataService.wrapWikidataIdWithApi(wikidataAssociationCreation.wikidataId);
    if (wikidataIdWithApi == null) {
      note.setWikidataId(null);
    } else {
      try {
        wikidataIdWithApi.associateNoteToWikidata(note, noteService);
      } catch (DuplicateWikidataIdException e) {
        BindingResult bindingResult =
            new BeanPropertyBindingResult(
                wikidataAssociationCreation, "wikidataAssociationCreation");
        bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
        throw new BindException(bindingResult);
      }
    }
    entityPersister.save(note);
    return noteRealmService.build(note, authorizationService.getCurrentUser());
  }

  @GetMapping("/{note}")
  public NoteRealm showNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();
    return noteRealmService.build(note, user);
  }

  @PatchMapping(
      path = "/{note}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public NoteAccessory updateNoteAccessories(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @ModelAttribute NoteAccessoriesDTO noteAccessoriesDTO)
      throws UnexpectedNoAccessRightException, IOException {
    authorizationService.assertAuthorization(note);
    return noteService.updateNoteAccessories(
        note, noteAccessoriesDTO, authorizationService.getCurrentUser());
  }

  @GetMapping("/{note}/accessory")
  public NoteAccessory showNoteAccessory(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    return note.getOrInitializeNoteAccessory();
  }

  @GetMapping("/{note}/note-info")
  public NoteRecallInfo getNoteInfo(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    NoteRecallInfo noteRecallInfo = new NoteRecallInfo();
    noteRecallInfo.setMemoryTrackers(
        userService.getMemoryTrackersFor(authorizationService.getCurrentUser(), note));
    noteRecallInfo.setRecallSetting(note.getRecallSetting());
    return noteRecallInfo;
  }

  @PostMapping(value = "/{note}/delete")
  @Transactional
  public List<NoteRealm> deleteNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    noteService.destroy(note);
    entityPersister.flush();
    return List.of();
  }

  @PatchMapping(value = "/{note}/undo-delete")
  @Transactional
  public NoteRealm undoDeleteNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    noteService.restore(note);
    entityPersister.flush();

    return noteRealmService.build(note, authorizationService.getCurrentUser());
  }

  @PostMapping(value = "/{note}/recall-setting")
  @Transactional
  public RedirectToNoteResponse updateNoteRecallSetting(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteRecallSetting noteRecallSetting)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    boolean rememberSpellingChangedToTrue =
        Boolean.TRUE.equals(noteRecallSetting.getRememberSpelling())
            && !Boolean.TRUE.equals(note.getRecallSetting().getRememberSpelling());
    if (rememberSpellingChangedToTrue) {
      userService.removeMemoryTrackersForReassimilation(
          authorizationService.getCurrentUser(), note);
    }
    BeanUtils.copyProperties(noteRecallSetting, note.getRecallSetting());
    entityPersister.save(note);
    return RedirectToNoteResponse.forNote(note.getId());
  }

  @GetMapping("/recent")
  public List<NoteSearchResult> getRecentNotes() throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    return noteService.findRecentNotesByUser(authorizationService.getCurrentUser().getId()).stream()
        .map(note -> new NoteSearchResult(note, null))
        .toList();
  }

  @GetMapping("/{note}/graph")
  public GraphRAGResult getGraph(
      @PathVariable("note") @Schema(type = "integer") Note note, @RequestParam() int tokenLimit)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();

    return graphRAGService.retrieve(note, tokenLimit, user);
  }

  @PostMapping(value = "/{note}/verify-spelling")
  @Transactional(readOnly = true)
  public SpellingVerificationResult verifySpelling(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody AnswerSpellingDTO dto)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    return new SpellingVerificationResult(note.matchAnswer(dto.getSpellingAnswer()));
  }
}
