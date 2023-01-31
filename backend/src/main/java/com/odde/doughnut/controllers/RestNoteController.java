package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.SearchTermModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;
import javax.validation.Valid;
import lombok.SneakyThrows;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
class RestNoteController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;
  private HttpClientAdapter httpClientAdapter;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestNoteController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.httpClientAdapter = httpClientAdapter;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping(value = "/{note}/updateWikidataId")
  @Transactional
  @SneakyThrows
  public NoteRealm updateWikidataId(
      @PathVariable(name = "note") Note note,
      @RequestBody WikidataAssociationCreation wikidataAssociationCreation)
      throws BindException, UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    WikidataIdWithApi wikidataIdWithApi =
        associateToWikidata(note, wikidataAssociationCreation.wikidataId);
    wikidataIdWithApi.extractWikidataInfoToNote(note);
    modelFactoryService.noteRepository.save(note);
    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }

  @PostMapping(value = "/{parentNote}/create")
  @Transactional
  @SneakyThrows
  public NoteRealmWithPosition createNote(
      @PathVariable(name = "parentNote") Note parentNote,
      @Valid @ModelAttribute NoteCreation noteCreation)
      throws UnexpectedNoAccessRightException, BindException, InterruptedException {
    currentUser.assertAuthorization(parentNote);
    User user = currentUser.getEntity();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    Note note = parentNote.buildChildNote(user, currentUTCTimestamp, noteCreation.textContent);
    WikidataIdWithApi wikidataIdWithApi = associateToWikidata(note, noteCreation.wikidataId);
    wikidataIdWithApi.extractWikidataInfoToNote(note);
    note.buildLinkToParent(user, noteCreation.getLinkTypeToParent(), currentUTCTimestamp);
    modelFactoryService.noteRepository.save(note);

    Optional<String> countryOfOrigin = wikidataIdWithApi.getCountryOfOrigin(parentNote);
    if (countryOfOrigin.isPresent()) {
      createNote(note, createCountryOfOriginNoteCreation(countryOfOrigin.get()));
    }

    return NoteRealmWithPosition.fromNote(note, user);
  }

  @SneakyThrows
  private WikidataIdWithApi associateToWikidata(Note note, String wikidataId) {
    note.setWikidataId(wikidataId);
    modelFactoryService.toNoteModel(note).checkDuplicateWikidataId();
    return getWikidataService().wrapWikidataIdWithApi(wikidataId);
  }

  @SneakyThrows
  private NoteCreation createCountryOfOriginNoteCreation(String countryOfOrigin) {
    NoteCreation noteCreation = new NoteCreation();
    TextContent textContent = new TextContent();
    textContent.setTitle(countryOfOrigin);
    noteCreation.linkTypeToParent = Link.LinkType.RELATED_TO;
    noteCreation.setTextContent(textContent);

    return noteCreation;
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
    noteAccessories.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
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

  private WikidataService getWikidataService() {
    return new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
  }
}
