package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.*;
import com.odde.donut.entities.*;
import com.odde.donut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.donut.entities.repositories.RecallLogRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteMotionService;
import com.odde.donut.services.NoteRealmService;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.NoteTrashService;
import com.odde.donut.services.NoteTrashUndoService;
import com.odde.donut.services.PortablePathAuthoring;
import com.odde.donut.services.UserService;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextResult;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.focusContext.RetrievalConfig;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
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
  private final PortablePathAuthoring portablePathAuthoring;
  private final NoteMotionService noteMotionService;
  private final NoteTrashService noteTrashService;
  private final NoteTrashUndoService noteTrashUndoService;

  public NoteController(
      EntityPersister entityPersister,
      NoteService noteService,
      AuthorizationService authorizationService,
      UserService userService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      NoteRealmService noteRealmService,
      RecallLogRepository recallLogRepository,
      AssimilationSequenceSkipRepository skipRepository,
      PortablePathAuthoring portablePathAuthoring,
      NoteMotionService noteMotionService,
      NoteTrashService noteTrashService,
      NoteTrashUndoService noteTrashUndoService) {
    this.entityPersister = entityPersister;
    this.noteService = noteService;
    this.authorizationService = authorizationService;
    this.userService = userService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
    this.noteRealmService = noteRealmService;
    this.recallLogRepository = recallLogRepository;
    this.skipRepository = skipRepository;
    this.portablePathAuthoring = portablePathAuthoring;
    this.noteMotionService = noteMotionService;
    this.noteTrashService = noteTrashService;
    this.noteTrashUndoService = noteTrashUndoService;
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
        recallLogRepository.findTutorLogsByMemoryTrackerId(tracker.getId()).stream()
            .findFirst()
            .map(RecallLog::getGrade)
            .map(Grade::getValue)
            .ifPresent(tracker::setLatestTutorFeedbackGrade);
      }
    }
    noteRecallInfo.setMemoryTrackers(memoryTrackers);
    noteRecallInfo.setSkippedPropertyKeys(
        skipRepository.findByUserAndNote(user, note).stream()
            .map(AssimilationSequenceSkip::getPropertyKey)
            .toList());
    return noteRecallInfo;
  }

  @PostMapping(value = "/{note}/trash")
  public NoteRealm trashNote(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteTrashDTO noteTrashDTO)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    return noteRealmService.build(
        noteTrashService.trash(
            note.getId(), note.getNotebook().getId(), noteTrashDTO.getReferenceHandling()),
        authorizationService.getCurrentUser());
  }

  @PatchMapping(value = "/{note}/undo-trash")
  public NoteRealm undoTrashNote(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteTrashUndoDTO undo)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    Folder priorFolder = noteTrashUndoService.priorFolder(undo.getPriorFolderId());
    Notebook destinationNotebook =
        priorFolder == null ? note.getNotebook() : priorFolder.getNotebook();
    authorizationService.assertAuthorization(destinationNotebook);
    if (!Objects.equals(note.getNotebook().getId(), destinationNotebook.getId())) {
      noteMotionService.executePlacement(
          note, destinationNotebook, priorFolder, undo.getPriorTitle());
      return noteRealmService.build(note, authorizationService.getCurrentUser());
    }
    return noteRealmService.build(
        noteTrashUndoService.undoSameNotebook(
            note.getId(),
            note.getNotebook().getId(),
            undo.getPriorFolderId(),
            undo.getPriorTitle()),
        authorizationService.getCurrentUser());
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
   * com.odde.donut.services.focusContext.FocusContextMarkdownRenderer}). {@code tokenLimit} is the
   * combined focus-plus-related content budget, matching {@link #getGraph(Note, int)}.
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

  @GetMapping("/{note}/authored-portable-path")
  public AuthoredPortablePath authoredPortablePath(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @RequestParam @Schema(type = "integer") Note destinationNote,
      @RequestParam(required = false) String portablePath)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    authorizationService.assertReadAuthorization(destinationNote);
    return new AuthoredPortablePath(
        portablePathAuthoring.authoredPortablePath(note, destinationNote, portablePath));
  }
}
