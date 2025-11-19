package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.DuplicateWikidataIdException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GraphRAGService;
import com.odde.doughnut.services.NoteMotionService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.services.graphRAG.CharacterBasedTokenCountingStrategy;
import com.odde.doughnut.services.graphRAG.FocusNote;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
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

  private final ModelFactoryService modelFactoryService;
  private final WikidataService wikidataService;
  private final TestabilitySettings testabilitySettings;
  private final NoteMotionService noteMotionService;
  private final NoteService noteService;
  private final AuthorizationService authorizationService;

  public NoteController(
      ModelFactoryService modelFactoryService,
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings,
      NoteMotionService noteMotionService,
      NoteService noteService,
      AuthorizationService authorizationService) {
    this.modelFactoryService = modelFactoryService;
    this.testabilitySettings = testabilitySettings;
    this.noteMotionService = noteMotionService;
    this.noteService = noteService;
    this.authorizationService = authorizationService;
    this.wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
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
    modelFactoryService.save(note);
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

    final User user = authorizationService.getCurrentUser();
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    note.getOrInitializeNoteAccessory().setFromDTO(noteAccessoriesDTO, user);
    modelFactoryService.save(note);
    return note.getNoteAccessory();
  }

  @GetMapping("/{note}/accessory")
  public NoteAccessory showNoteAccessory(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    return note.getNoteAccessory();
  }

  @GetMapping("/{note}/note-info")
  public NoteInfo getNoteInfo(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    NoteInfo noteInfo = new NoteInfo();
    noteInfo.setMemoryTrackers(
        modelFactoryService
            .toUserModel(authorizationService.getCurrentUser())
            .getMemoryTrackersFor(note));
    noteInfo.setNote(note.toNoteRealm(authorizationService.getCurrentUser()));
    noteInfo.setCreatedAt(note.getCreatedAt());
    noteInfo.setRecallSetting(note.getRecallSetting());
    return noteInfo;
  }

  @PostMapping(value = "/{note}/delete")
  @Transactional
  public List<NoteRealm> deleteNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    noteService.destroy(note, testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.entityManager.flush();
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
    modelFactoryService.entityManager.flush();

    return note.toNoteRealm(authorizationService.getCurrentUser());
  }

  @PostMapping(value = "/{note}/review-setting")
  @Transactional
  public RedirectToNoteResponse updateRecallSetting(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody RecallSetting recallSetting)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    BeanUtils.copyProperties(recallSetting, note.getRecallSetting());
    modelFactoryService.save(note);
    note.getLinksAndRefers()
        .forEach(
            link -> {
              link.getRecallSetting()
                  .setLevel(Math.max(link.getRecallSetting().getLevel(), recallSetting.getLevel()));
              modelFactoryService.save(link);
            });
    return new RedirectToNoteResponse(note.getId());
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
  public List<NoteRealm> getRecentNotes() throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    return modelFactoryService
        .noteRepository
        .findRecentNotesByUser(authorizationService.getCurrentUser().getId())
        .stream()
        .map(note -> note.toNoteRealm(authorizationService.getCurrentUser()))
        .toList();
  }

  @GetMapping("/{note}/graph")
  public GraphRAGResult getGraph(
      @PathVariable("note") @Schema(type = "integer") Note note, @RequestParam() int tokenLimit)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);

    GraphRAGService graphRAGService =
        new GraphRAGService(new CharacterBasedTokenCountingStrategy());
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
}
