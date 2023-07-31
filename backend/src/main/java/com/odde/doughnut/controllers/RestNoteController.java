package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.SearchTermModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class RestNoteController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;
  private final WikidataService wikidataService;
  private final TestabilitySettings testabilitySettings;

  public RestNoteController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
  }

  @PostMapping(value = "/{note}/updateWikidataId")
  @Transactional
  public NoteRealm updateWikidataId(
      @PathVariable(name = "note") Note note,
      @RequestBody WikidataAssociationCreation wikidataAssociationCreation)
      throws BindException, UnexpectedNoAccessRightException, IOException, InterruptedException {
    currentUser.assertAuthorization(note);
    WikidataIdWithApi wikidataIdWithApi =
        wikidataService.wrapWikidataIdWithApi(wikidataAssociationCreation.wikidataId);
    wikidataIdWithApi.associateNoteToWikidata(note, modelFactoryService);
    modelFactoryService.noteRepository.save(note);
    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }

  @PostMapping(value = "/{parentNote}/create")
  @Transactional
  public NoteRealmWithPosition createNote(
      @PathVariable(name = "parentNote") Note parentNote,
      @Valid @ModelAttribute NoteCreation noteCreation)
      throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
    currentUser.assertAuthorization(parentNote);
    User user = currentUser.getEntity();

    Note note =
        getNoteConstructionService(user)
            .createNoteWithWikidataInfo(
                parentNote,
                wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId),
                noteCreation.textContent,
                noteCreation.getLinkTypeToParent());

    return NoteRealmWithPosition.fromNote(note, user);
  }

  private NoteConstructionService getNoteConstructionService(User user) {
    return new NoteConstructionService(
        user, testabilitySettings.getCurrentUTCTimestamp(), modelFactoryService);
  }

  @GetMapping("/{note}")
  public NoteRealmWithPosition show(@PathVariable("note") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    return NoteRealmWithPosition.fromNote(note, currentUser.getEntity());
  }

  @GetMapping("/{note}/overview")
  public NoteRealmWithAllDescendants showOverview(@PathVariable("note") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    return NoteRealmWithAllDescendants.fromNote(note, currentUser.getEntity());
  }

  @PatchMapping(path = "/{note}")
  @Transactional
  public NoteRealm updateNote(
      @PathVariable(name = "note") Note note,
      @Valid @ModelAttribute NoteAccessories noteAccessories)
      throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertAuthorization(note);

    final User user = currentUser.getEntity();
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    note.updateNoteContent(noteAccessories, user);
    modelFactoryService.noteRepository.save(note);
    return new NoteViewer(user, note).toJsonObject();
  }

  @GetMapping("/{note}/note-info")
  public NoteInfo getNoteInfo(@PathVariable("note") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    NoteInfo noteInfo = new NoteInfo();
    noteInfo.setReviewPoint(currentUser.getReviewPointFor(note));
    noteInfo.setNote(new NoteViewer(currentUser.getEntity(), note).toJsonObject());
    noteInfo.setCreatedAt(note.getThing().getCreatedAt());
    noteInfo.setReviewSetting(note.getMasterReviewSetting());
    return noteInfo;
  }

  @PostMapping("/search")
  @Transactional
  public List<Note> searchForLinkTarget(@Valid @RequestBody SearchTerm searchTerm) {
    SearchTermModel searchTermModel =
        modelFactoryService.toSearchTermModel(currentUser.getEntity(), searchTerm);
    return searchTermModel.searchForNotes();
  }

  @PostMapping(value = "/{note}/delete")
  @Transactional
  public List<NoteRealm> deleteNote(@PathVariable("note") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    modelFactoryService.toNoteModel(note).destroy(testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.entityManager.flush();
    Note parentNote = note.getParentNote();
    if (parentNote != null) {
      return List.of(new NoteViewer(currentUser.getEntity(), parentNote).toJsonObject());
    }
    return List.of();
  }

  @PatchMapping(value = "/{note}/undo-delete")
  @Transactional
  public NoteRealm undoDeleteNote(@PathVariable("note") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    modelFactoryService.toNoteModel(note).restore();
    modelFactoryService.entityManager.flush();

    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }

  @GetMapping("/{note}/position")
  public NotePositionViewedByUser getPosition(Note note) throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    return new NoteViewer(currentUser.getEntity(), note).jsonNotePosition();
  }

  @PostMapping(value = "/{note}/review-setting")
  @Transactional
  public RedirectToNoteResponse updateReviewSetting(
      @PathVariable("note") Note note, @Valid @RequestBody ReviewSetting reviewSetting)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    note.mergeMasterReviewSetting(reviewSetting);
    modelFactoryService.noteRepository.save(note);
    return new RedirectToNoteResponse(note.getId());
  }
}
