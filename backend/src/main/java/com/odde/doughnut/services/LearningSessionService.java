package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.CommissionedLearningSessionFeedbackScheduling;
import com.odde.doughnut.controllers.dto.LearningSessionRequestResponse;
import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.controllers.dto.RecordedLearningSessionItem;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.LearningSessionRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
import com.odde.doughnut.services.LearningSessionReportParser.ParseResult;
import com.odde.doughnut.services.LearningSessionReportParser.ParsedReportEntry;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearningSessionService {

  private final UserService userService;
  private final LearningSessionRepository learningSessionRepository;
  private final SessionItemRepository sessionItemRepository;
  private final NoteRepository noteRepository;
  private final LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder;
  private final LearningSessionReportParser learningSessionReportParser;
  private final MemoryTrackerService memoryTrackerService;

  @Autowired
  public LearningSessionService(
      UserService userService,
      LearningSessionRepository learningSessionRepository,
      SessionItemRepository sessionItemRepository,
      NoteRepository noteRepository,
      LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder,
      LearningSessionReportParser learningSessionReportParser,
      MemoryTrackerService memoryTrackerService) {
    this.userService = userService;
    this.learningSessionRepository = learningSessionRepository;
    this.sessionItemRepository = sessionItemRepository;
    this.noteRepository = noteRepository;
    this.learningSessionRequestMarkdownBuilder = learningSessionRequestMarkdownBuilder;
    this.learningSessionReportParser = learningSessionReportParser;
    this.memoryTrackerService = memoryTrackerService;
  }

  @Transactional(readOnly = true)
  public LearningSessionRequestResponse request(
      User user, Notebook notebook, Timestamp now, ZoneId zoneId) {
    List<MemoryTracker> dueTrackers =
        requireDueCommissionedNoteLevelTrackers(user, notebook, now, zoneId);

    LearningSessionRequestResponse response = new LearningSessionRequestResponse();
    response.setRequestMarkdown(
        learningSessionRequestMarkdownBuilder.build(user, notebook, dueTrackers, zoneId));
    return response;
  }

  @Transactional
  public RecordLearningSessionResponse record(
      User user, Notebook notebook, String reportMarkdown, Timestamp now) {
    List<Note> notebookNotes =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    List<String> notebookTitleList = notebookNotes.stream().map(Note::getTitle).toList();
    Set<String> notebookTitles = Set.copyOf(notebookTitleList);
    Set<String> ambiguousTitles = LearningSessionReportParser.ambiguousTitles(notebookTitleList);
    ParseResult parseResult =
        learningSessionReportParser.parse(reportMarkdown, notebookTitles, ambiguousTitles);

    RecordLearningSessionResponse response = new RecordLearningSessionResponse();
    response.setRecordedItems(new ArrayList<>());
    response.setRejectedEntries(
        new ArrayList<>(
            RejectedLearningSessionReportEntryMapper.fromParsed(parseResult.rejected())));

    List<MatchedReportEntry> matchedEntries = new ArrayList<>();
    for (ParsedReportEntry entry : parseResult.entries()) {
      Optional<MemoryTracker> tracker =
          findCommissionedNoteLevelTracker(user, notebookNotes, entry.noteTitle());
      if (tracker.isEmpty()) {
        response
            .getRejectedEntries()
            .add(
                RejectedLearningSessionReportEntryMapper.of(
                    entry.noteTitle() + ": " + entry.score(),
                    "No commissioned memory tracker for this note."));
        continue;
      }
      matchedEntries.add(new MatchedReportEntry(entry, tracker.get()));
    }

    if (matchedEntries.isEmpty()) {
      return response;
    }

    LearningSession session = new LearningSession();
    session.setUser(user);
    session.setNotebook(notebook);
    session.setRecordedAt(now);
    learningSessionRepository.save(session);

    for (MatchedReportEntry matched : matchedEntries) {
      int score = matched.entry().score();
      ProductOutcome productOutcome =
          CommissionedLearningSessionFeedbackScheduling.productOutcomeForScore(score);
      SessionItem item = createSessionItem(session, matched.tracker(), score, now);
      memoryTrackerService.persistRecallLog(matched.tracker(), now, productOutcome, null);
      CommissionedLearningSessionFeedbackScheduling.recordFeedback(
          matched.tracker(), now, productOutcome);
      sessionItemRepository.save(item);

      RecordedLearningSessionItem recorded = new RecordedLearningSessionItem();
      recorded.setNoteTitle(matched.entry().noteTitle());
      recorded.setScore(score);
      recorded.setMemoryTrackerId(matched.tracker().getId());
      response.getRecordedItems().add(recorded);
    }

    response.setRecordedAt(now);
    return response;
  }

  private Optional<MemoryTracker> findCommissionedNoteLevelTracker(
      User user, List<Note> notebookNotes, String noteTitle) {
    return notebookNotes.stream()
        .filter(note -> note.getTitle().equals(noteTitle))
        .flatMap(note -> userService.getMemoryTrackersFor(user, note).stream())
        .filter(MemoryTracker::isCommissioned)
        .filter(MemoryTracker::isNoteLevelTracker)
        .findFirst();
  }

  private List<MemoryTracker> requireDueCommissionedNoteLevelTrackers(
      User user, Notebook notebook, Timestamp now, ZoneId zoneId) {
    List<MemoryTracker> dueTrackers =
        userService
            .getCommissionedMemoryTrackersNeedToRepeat(user, now, zoneId)
            .filter(tracker -> tracker.getNote().getNotebook().getId().equals(notebook.getId()))
            .filter(MemoryTracker::isNoteLevelTracker)
            .toList();
    if (dueTrackers.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "No due commissioned memory trackers for this notebook.");
    }
    return dueTrackers;
  }

  private SessionItem createSessionItem(
      LearningSession session, MemoryTracker tracker, int score, Timestamp now) {
    SessionItem item = new SessionItem();
    item.setLearningSession(session);
    item.setMemoryTracker(tracker);
    item.setNoteTitle(tracker.getNote().getTitle());
    item.setFeedbackScore(score);
    item.setFeedbackRecordedAt(now);
    return item;
  }

  private record MatchedReportEntry(ParsedReportEntry entry, MemoryTracker tracker) {}
}
