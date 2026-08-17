package com.odde.doughnut.controllers;

import com.odde.doughnut.algorithms.CommissionedLearningSessionFeedbackScheduling;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.doughnut.entities.repositories.RecallLogRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteRealmService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class NoteController {

  private final EntityPersister entityPersister;
  private final NoteService noteService;
  private final AuthorizationService authorizationService;
  private final UserService userService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  private final NoteRealmService noteRealmService;
  private final RecallLogRepository recallLogRepository;
  private final AssimilationSequenceSkipRepository skipRepository;

  public NoteController(
      EntityPersister entityPersister,
      NoteService noteService,
      AuthorizationService authorizationService,
      UserService userService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      NoteRealmService noteRealmService,
      RecallLogRepository recallLogRepository,
      AssimilationSequenceSkipRepository skipRepository) {
    this.entityPersister = entityPersister;
    this.noteService = noteService;
    this.authorizationService = authorizationService;
    this.userService = userService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
    this.noteRealmService = noteRealmService;
    this.recallLogRepository = recallLogRepository;
    this.skipRepository = skipRepository;
  }

  @GetMapping("/{note}")
  public NoteRealm showNote(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();
    return noteRealmService.build(note, user);
  }

  @PostMapping(value = "/{note}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Transactional
  public NoteImageUploadResult uploadNoteImage(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @ModelAttribute NoteImageUploadDTO noteImageUploadDTO)
      throws UnexpectedNoAccessRightException, IOException {
    authorizationService.assertAuthorization(note);
    return noteService.uploadNoteImage(
        note, noteImageUploadDTO, authorizationService.getCurrentUser());
  }

  @GetMapping("/{note}/note-info")
  public NoteRecallInfo getNoteInfo(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();
    NoteRecallInfo noteRecallInfo = new NoteRecallInfo();
    List<MemoryTracker> memoryTrackers = userService.getMemoryTrackersFor(user, note);
    for (MemoryTracker tracker : memoryTrackers) {
      if (tracker.isCommissioned()) {
        recallLogRepository
            .findLatestTutorProductOutcomeByMemoryTrackerId(tracker.getId())
            .map(CommissionedLearningSessionFeedbackScheduling::scoreForProductOutcome)
            .ifPresent(tracker::setLatestTutorFeedbackScore);
      }
    }
    noteRecallInfo.setMemoryTrackers(memoryTrackers);
    noteRecallInfo.setRecallSetting(note.getRecallSetting());
    noteRecallInfo.setSkippedPropertyKeys(
        skipRepository.findByUserAndNote(user, note).stream()
            .map(AssimilationSequenceSkip::getPropertyKey)
            .toList());
    return noteRecallInfo;
  }

  @PostMapping(value = "/{note}/delete")
  @Transactional
  public List<NoteRealm> deleteNote(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteDeleteDTO noteDeleteDTO)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    noteService.destroy(
        note,
        noteDeleteDTO.getReferenceHandling(),
        noteDeleteDTO.getSourcePropertyKey(),
        authorizationService.getCurrentUser());
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
  public FocusContextResult getGraph(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Parameter(
              description =
                  "Approximate token budget for focus note content plus related note content combined (bodies).")
          @RequestParam()
          int tokenLimit)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();

    return focusContextRetrievalService.retrieve(
        note, user, RetrievalConfig.forGraphApi(tokenLimit));
  }

  /**
   * Focus-context markdown for this note (same render as {@link
   * com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer}). {@code tokenLimit} is
   * the combined focus-plus-related content budget, matching {@link #getGraph(Note, int)}.
   */
  @GetMapping("/{note}/ai-context-markdown")
  public NoteAiContextMarkdown getAiContextMarkdown(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Parameter(
              description =
                  "Approximate token budget for focus note content plus related note content combined (bodies).")
          @RequestParam()
          int tokenLimit)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();
    RetrievalConfig config = RetrievalConfig.forGraphApi(tokenLimit);
    FocusContextResult focusContextResult =
        focusContextRetrievalService.retrieve(note, user, config);
    String markdown = focusContextMarkdownRenderer.render(focusContextResult, config);
    return new NoteAiContextMarkdown(markdown);
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
