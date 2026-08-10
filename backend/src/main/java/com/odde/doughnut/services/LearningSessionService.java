package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.CommissionedLearningSessionFeedbackScheduling;
import com.odde.doughnut.controllers.dto.LearningSessionCommissionResponse;
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

  @Autowired
  public LearningSessionService(
      UserService userService,
      LearningSessionRepository learningSessionRepository,
      SessionItemRepository sessionItemRepository,
      NoteRepository noteRepository,
      LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder,
      LearningSessionReportParser learningSessionReportParser) {
    this.userService = userService;
    this.learningSessionRepository = learningSessionRepository;
    this.sessionItemRepository = sessionItemRepository;
    this.noteRepository = noteRepository;
    this.learningSessionRequestMarkdownBuilder = learningSessionRequestMarkdownBuilder;
    this.learningSessionReportParser = learningSessionReportParser;
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
  public LearningSessionCommissionResponse commission(
      User user, Notebook notebook, Timestamp now, ZoneId zoneId) {
    List<MemoryTracker> dueTrackers =
        requireDueCommissionedNoteLevelTrackers(user, notebook, now, zoneId);

    abandonUnfinishedSessions(user, notebook);

    LearningSession session = new LearningSession();
    session.setUser(user);
    session.setNotebook(notebook);
    session.setStatus(LearningSessionStatus.AWAITING_REPORT);
    session.setCommissionedAt(now);
    learningSessionRepository.save(session);

    for (MemoryTracker tracker : dueTrackers) {
      sessionItemRepository.save(createSessionItem(session, tracker));
    }

    return toCommissionResponse(session, dueTrackers, zoneId);
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
    session.setStatus(LearningSessionStatus.RECORDED);
    session.setCommissionedAt(now);
    session.setRecordedAt(now);
    learningSessionRepository.save(session);

    for (MatchedReportEntry matched : matchedEntries) {
      SessionItem item = createSessionItem(session, matched.tracker());
      item.setFeedbackScore(matched.entry().score());
      item.setFeedbackRecordedAt(now);
      CommissionedLearningSessionFeedbackScheduling.recordFeedback(
          matched.tracker(), now, matched.entry().score());
      sessionItemRepository.save(item);

      RecordedLearningSessionItem recorded = new RecordedLearningSessionItem();
      recorded.setNoteTitle(matched.entry().noteTitle());
      recorded.setScore(matched.entry().score());
      recorded.setMemoryTrackerId(matched.tracker().getId());
      response.getRecordedItems().add(recorded);
    }

    response.setStatus(LearningSessionStatus.RECORDED);
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

  private void abandonUnfinishedSessions(User user, Notebook notebook) {
    List<LearningSession> unfinished =
        learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
            user.getId(), notebook.getId(), LearningSessionStatus.AWAITING_REPORT);
    for (LearningSession session : unfinished) {
      sessionItemRepository.deleteByLearningSession_Id(session.getId());
    }
    learningSessionRepository.deleteAll(unfinished);
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

  private SessionItem createSessionItem(LearningSession session, MemoryTracker tracker) {
    SessionItem item = new SessionItem();
    item.setLearningSession(session);
    item.setMemoryTracker(tracker);
    item.setNoteTitle(tracker.getNote().getTitle());
    return item;
  }

  private LearningSessionCommissionResponse toCommissionResponse(
      LearningSession session, List<MemoryTracker> trackers, ZoneId zoneId) {
    LearningSessionCommissionResponse response = new LearningSessionCommissionResponse();
    response.setLearningSessionId(session.getId());
    response.setRequestMarkdown(
        learningSessionRequestMarkdownBuilder.build(
            session.getUser(), session.getNotebook(), trackers, zoneId));
    response.setStatus(session.getStatus());
    return response;
  }

  private record MatchedReportEntry(ParsedReportEntry entry, MemoryTracker tracker) {}
}
