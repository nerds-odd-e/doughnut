package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.DuplicateWikidataIdException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteMotionModel;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.SearchTermModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.WikidataService;
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
class RestNoteController {

  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;
  private final WikidataService wikidataService;
  private final TestabilitySettings testabilitySettings;
  private final ConversationService conversationService;

  public RestNoteController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings,
      ConversationService conversationService) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.conversationService = conversationService;
    this.wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
  }

  @PostMapping(value = "/{note}/updateWikidataId")
  @Transactional
  public NoteRealm updateWikidataId(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @RequestBody WikidataAssociationCreation wikidataAssociationCreation)
      throws BindException, UnexpectedNoAccessRightException, IOException, InterruptedException {
    currentUser.assertAuthorization(note);
    WikidataIdWithApi wikidataIdWithApi =
        wikidataService.wrapWikidataIdWithApi(wikidataAssociationCreation.wikidataId);
    try {
      wikidataIdWithApi.associateNoteToWikidata(note, modelFactoryService);
    } catch (DuplicateWikidataIdException e) {
      BindingResult bindingResult =
          new BeanPropertyBindingResult(wikidataAssociationCreation, "wikidataAssociationCreation");
      bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
      throw new BindException(bindingResult);
    }
    modelFactoryService.save(note);
    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }

  @PostMapping(value = "/{parentNote}/create")
  @Transactional
  public NoteCreationRresult createNote(
      @PathVariable(name = "parentNote") @Schema(type = "integer") Note parentNote,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    currentUser.assertAuthorization(parentNote);
    User user = currentUser.getEntity();

    try {
      Note note =
          getNoteConstructionService(user)
              .createNoteWithWikidataInfo(
                  parentNote,
                  wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId),
                  noteCreation.getLinkTypeToParent(),
                  noteCreation.getTopicConstructor());
      return new NoteCreationRresult(
          new NoteViewer(user, note).toJsonObject(),
          new NoteViewer(user, parentNote).toJsonObject());
    } catch (DuplicateWikidataIdException e) {
      BindingResult bindingResult = new BeanPropertyBindingResult(noteCreation, "noteCreation");
      bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
      throw new BindException(bindingResult);
    }
  }

  private NoteConstructionService getNoteConstructionService(User user) {
    return new NoteConstructionService(
        user, testabilitySettings.getCurrentUTCTimestamp(), modelFactoryService);
  }

  @GetMapping("/{note}")
  public NoteRealm show(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    User user = currentUser.getEntity();
    return new NoteViewer(user, note).toJsonObject();
  }

  @PatchMapping(
      path = "/{note}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public NoteAccessory updateNoteAccessories(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @ModelAttribute NoteAccessoriesDTO noteAccessoriesDTO)
      throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertAuthorization(note);

    final User user = currentUser.getEntity();
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    note.getOrInitializeNoteAccessory().setFromDTO(noteAccessoriesDTO, user);
    modelFactoryService.save(note);
    return note.getNoteAccessory();
  }

  @PatchMapping(
      path = "/{note}/audio",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public NoteAccessory uploadAudio(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @ModelAttribute AudioUploadDTO audioUploadDTO)
      throws IOException {
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    final User user = currentUser.getEntity();
    note.getOrInitializeNoteAccessory().setAudio(audioUploadDTO, user);
    modelFactoryService.save(note.getNoteAccessory().getAudioAttachment());
    modelFactoryService.save(note);

    return note.getNoteAccessory();
  }

  @GetMapping("/{note}/accessory")
  public NoteAccessory showNoteAccessory(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    return note.getNoteAccessory();
  }

  @GetMapping("/{note}/note-info")
  public NoteInfo getNoteInfo(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    NoteInfo noteInfo = new NoteInfo();
    noteInfo.setReviewPoint(currentUser.getReviewPointFor(note));
    noteInfo.setNote(new NoteViewer(currentUser.getEntity(), note).toJsonObject());
    noteInfo.setCreatedAt(note.getCreatedAt());
    noteInfo.setReviewSetting(note.getReviewSetting());
    return noteInfo;
  }

  @PostMapping("/search")
  @Transactional
  public List<NoteTopic> searchForLinkTarget(@Valid @RequestBody SearchTerm searchTerm) {
    SearchTermModel searchTermModel =
        modelFactoryService.toSearchTermModel(currentUser.getEntity(), searchTerm);
    return searchTermModel.searchForNotes();
  }

  @PostMapping("/{note}/search")
  @Transactional
  public List<NoteTopic> searchForLinkTargetWithin(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody SearchTerm searchTerm) {
    SearchTermModel searchTermModel =
        modelFactoryService.toSearchTermModel(currentUser.getEntity(), searchTerm);
    return searchTermModel.searchForNotesInRelateTo(note);
  }

  @PostMapping(value = "/{note}/delete")
  @Transactional
  public List<NoteRealm> deleteNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    modelFactoryService.toNoteModel(note).destroy(testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.entityManager.flush();
    Note parentNote = note.getParent();
    if (parentNote != null) {
      return List.of(new NoteViewer(currentUser.getEntity(), parentNote).toJsonObject());
    }
    return List.of();
  }

  @PatchMapping(value = "/{note}/undo-delete")
  @Transactional
  public NoteRealm undoDeleteNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    modelFactoryService.toNoteModel(note).restore();
    modelFactoryService.entityManager.flush();

    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }

  @PostMapping(value = "/{note}/review-setting")
  @Transactional
  public RedirectToNoteResponse updateReviewSetting(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody ReviewSetting reviewSetting)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    BeanUtils.copyProperties(reviewSetting, note.getReviewSetting());
    modelFactoryService.save(note);
    note.getLinksAndRefers()
        .forEach(
            link -> {
              link.getReviewSetting()
                  .setLevel(Math.max(link.getReviewSetting().getLevel(), reviewSetting.getLevel()));
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
    currentUser.assertAuthorization(note);
    currentUser.assertAuthorization(targetNote);

    boolean asFirstChildBoolean = asFirstChild.compareToIgnoreCase("asFirstChild") == 0;
    NoteMotionModel noteMotion =
        modelFactoryService.motionOfMoveAfter(note, targetNote, asFirstChildBoolean);
    noteMotion.validate();
    Note parentBefore = note.getParent();
    noteMotion.execute();

    return Stream.of(parentBefore, note.getParent())
        .distinct()
        .map(parent -> new NoteViewer(currentUser.getEntity(), parent).toJsonObject())
        .toList();
  }

  @PostMapping("/{note}/conversation")
  public Conversation sendNoteFeedback(
      @PathVariable("note") @Schema(type = "integer") Note note, @RequestBody String message) {
    Conversation conversation =
        conversationService.startConversationOfNote(note, currentUser.getEntity());
    conversationService.addMessageToConversation(conversation, currentUser.getEntity(), message);
    return conversation;
  }
}
