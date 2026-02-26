package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.DuplicateWikidataIdException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GraphRAGService;
import com.odde.doughnut.services.NoteMotionService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.services.graphRAG.FocusNote;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
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
  private final NoteMotionService noteMotionService;
  private final NoteService noteService;
  private final AuthorizationService authorizationService;
  private final UserService userService;
  private final GraphRAGService graphRAGService;
  private final TestabilitySettings testabilitySettings;

  public NoteController(
      EntityPersister entityPersister,
      WikidataService wikidataService,
      NoteMotionService noteMotionService,
      NoteService noteService,
      AuthorizationService authorizationService,
      UserService userService,
      GraphRAGService graphRAGService,
      TestabilitySettings testabilitySettings) {
    this.entityPersister = entityPersister;
    this.wikidataService = wikidataService;
    this.noteMotionService = noteMotionService;
    this.noteService = noteService;
    this.authorizationService = authorizationService;
    this.userService = userService;
    this.graphRAGService = graphRAGService;
    this.testabilitySettings = testabilitySettings;
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
    try {
      wikidataIdWithApi.associateNoteToWikidata(note, noteService);
    } catch (DuplicateWikidataIdException e) {
      BindingResult bindingResult =
          new BeanPropertyBindingResult(wikidataAssociationCreation, "wikidataAssociationCreation");
      bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
      throw new BindException(bindingResult);
    }
    entityPersister.save(note);
    return note.toNoteRealm(authorizationService.getCurrentUser());
  }

  @GetMapping("/{note}")
  public NoteRealm showNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();
    return note.toNoteRealm(user);
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
  public NoteInfo getNoteInfo(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    NoteInfo noteInfo = new NoteInfo();
    noteInfo.setMemoryTrackers(
        userService.getMemoryTrackersFor(authorizationService.getCurrentUser(), note));
    noteInfo.setNote(note.toNoteRealm(authorizationService.getCurrentUser()));
    noteInfo.setCreatedAt(note.getCreatedAt());
    noteInfo.setRecallSetting(note.getRecallSetting());
    noteInfo.setNoteType(note.getNoteType());
    return noteInfo;
  }

  @PostMapping(value = "/{note}/delete")
  @Transactional
  public List<NoteRealm> deleteNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    noteService.destroy(note);
    entityPersister.flush();
    Note parentNote = note.getParent();
    if (parentNote != null) {
      return List.of(parentNote.toNoteRealm(authorizationService.getCurrentUser()));
    }
    return List.of();
  }

  @PatchMapping(value = "/{note}/undo-delete")
  @Transactional
  public NoteRealm undoDeleteNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    noteService.restore(note);
    entityPersister.flush();

    return note.toNoteRealm(authorizationService.getCurrentUser());
  }

  @PostMapping(value = "/{note}/recall-setting")
  @Transactional
  public RedirectToNoteResponse updateNoteRecallSetting(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteRecallSetting noteRecallSetting)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    BeanUtils.copyProperties(noteRecallSetting, note.getRecallSetting());
    entityPersister.save(note);
    note.getRelationshipsAndRefers()
        .forEach(
            relation -> {
              relation
                  .getRecallSetting()
                  .setLevel(
                      Math.max(
                          relation.getRecallSetting().getLevel(), noteRecallSetting.getLevel()));
              entityPersister.save(relation);
            });
    return new RedirectToNoteResponse(note.getId());
  }

  @PatchMapping(value = "/{note}/note-type")
  @Transactional
  public NoteRealm updateNoteType(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @RequestBody(required = false) NoteType noteType)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    noteService.setNoteType(note, noteType);
    return note.toNoteRealm(authorizationService.getCurrentUser());
  }

  @PostMapping(value = "/move_after/{note}/{targetNote}/{asFirstChild}")
  @Transactional
  public List<NoteRealm> moveAfter(
      @PathVariable @Schema(type = "integer") Note note,
      @PathVariable @Schema(type = "integer") Note targetNote,
      @PathVariable @Schema(type = "string") String asFirstChild)
      throws UnexpectedNoAccessRightException,
          CyclicLinkDetectedException,
          MovementNotPossibleException {
    authorizationService.assertAuthorization(note);
    authorizationService.assertAuthorization(targetNote);

    boolean asFirstChildBoolean = asFirstChild.compareToIgnoreCase("asFirstChild") == 0;
    noteMotionService.validate(note, targetNote, asFirstChildBoolean);
    Note parentBefore = note.getParent();
    noteMotionService.execute(note, targetNote, asFirstChildBoolean);

    return Stream.of(parentBefore, note.getParent())
        .distinct()
        .map(parent -> parent.toNoteRealm(authorizationService.getCurrentUser()))
        .toList();
  }

  @GetMapping("/recent")
  public List<NoteSearchResult> getRecentNotes() throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    return noteService.findRecentNotesByUser(authorizationService.getCurrentUser().getId()).stream()
        .map(note -> new NoteSearchResult(note.getNoteTopology(), note.getNotebook().getId(), null))
        .toList();
  }

  @GetMapping("/{note}/graph")
  public GraphRAGResult getGraph(
      @PathVariable("note") @Schema(type = "integer") Note note, @RequestParam() int tokenLimit)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);

    return graphRAGService.retrieve(note, tokenLimit);
  }

  @GetMapping("/{note}/descendants")
  public GraphRAGResult getDescendants(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    FocusNote focus = FocusNote.fromNote(note);
    List<BareNote> descendants =
        note.getAllDescendants().map(BareNote::fromNoteWithoutTruncate).toList();
    GraphRAGResult result = new GraphRAGResult(focus);
    result.getRelatedNotes().addAll(descendants);
    return result;
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
