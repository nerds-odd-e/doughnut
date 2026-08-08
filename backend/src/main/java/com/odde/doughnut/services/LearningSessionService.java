package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.CommissionedLearningSessionFeedbackScheduling;
import com.odde.doughnut.controllers.dto.LearningSessionCommissionResponse;
import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.controllers.dto.RecordedLearningSessionItem;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.LearningSessionRepository;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
import com.odde.doughnut.services.LearningSessionReportParser.ParseResult;
import com.odde.doughnut.services.LearningSessionReportParser.ParsedReportEntry;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
  private final LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder;
  private final LearningSessionReportParser learningSessionReportParser;
  private final LearningSessionRecordTargetResolver learningSessionRecordTargetResolver;

  @Autowired
  public LearningSessionService(
      UserService userService,
      LearningSessionRepository learningSessionRepository,
      SessionItemRepository sessionItemRepository,
      LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder,
      LearningSessionReportParser learningSessionReportParser,
      LearningSessionRecordTargetResolver learningSessionRecordTargetResolver) {
    this.userService = userService;
    this.learningSessionRepository = learningSessionRepository;
    this.sessionItemRepository = sessionItemRepository;
    this.learningSessionRequestMarkdownBuilder = learningSessionRequestMarkdownBuilder;
    this.learningSessionReportParser = learningSessionReportParser;
    this.learningSessionRecordTargetResolver = learningSessionRecordTargetResolver;
  }

  @Transactional
  public LearningSessionCommissionResponse commission(
      User user, Notebook notebook, Timestamp now, ZoneId zoneId) {
    List<MemoryTracker> dueTrackers = dueCommissionedNoteLevelTrackers(user, notebook, now, zoneId);

    if (dueTrackers.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "No due commissioned memory trackers for this notebook.");
    }

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

    return toCommissionResponse(session, zoneId);
  }

  @Transactional
  public RecordLearningSessionResponse record(
      User user,
      Notebook notebook,
      String reportMarkdown,
      Integer learningSessionId,
      Timestamp now) {
    LearningSessionRecordTargetResolver.LearningSessionRecordTarget target =
        learningSessionRecordTargetResolver.resolve(user, notebook, learningSessionId);
    LearningSession session = target.session();
    boolean isAmend = target.isAmend();
    List<SessionItem> sessionItems =
        sessionItemRepository.findByLearningSession_Id(session.getId());
    Set<String> sessionItemTitles =
        sessionItems.stream().map(SessionItem::getNoteTitle).collect(Collectors.toSet());
    Set<String> ambiguousTitles =
        LearningSessionReportParser.ambiguousTitles(
            sessionItems.stream().map(SessionItem::getNoteTitle).toList());
    ParseResult parseResult =
        learningSessionReportParser.parse(reportMarkdown, sessionItemTitles, ambiguousTitles);

    RecordLearningSessionResponse response = new RecordLearningSessionResponse();
    response.setStatus(session.getStatus());
    response.setRecordedItems(new ArrayList<>());
    response.setRejectedEntries(
        new ArrayList<>(
            RejectedLearningSessionReportEntryMapper.fromParsed(parseResult.rejected())));

    for (ParsedReportEntry entry : parseResult.entries()) {
      SessionItem matched =
          sessionItems.stream()
              .filter(item -> item.getNoteTitle().equals(entry.noteTitle()))
              .findFirst()
              .orElseThrow();

      if (isAmend && matched.getPreSessionRecallCount() == null) {
        response
            .getRejectedEntries()
            .add(
                RejectedLearningSessionReportEntryMapper.of(
                    entry.noteTitle() + ": " + entry.score(),
                    "Cannot amend: no pre-session snapshot for this item."));
        continue;
      }

      matched.setFeedbackScore(entry.score());
      matched.setFeedbackRecordedAt(now);
      MemoryTracker tracker = matched.getMemoryTracker();
      if (isAmend) {
        CommissionedLearningSessionFeedbackScheduling.restorePreSessionSnapshot(tracker, matched);
      } else if (matched.getPreSessionRecallCount() == null) {
        matched.setPreSessionForgettingCurveIndex(tracker.getForgettingCurveIndex());
        matched.setPreSessionRecallCount(tracker.getRecallCount());
      }
      CommissionedLearningSessionFeedbackScheduling.recordFeedback(tracker, now, entry.score());
      sessionItemRepository.save(matched);

      RecordedLearningSessionItem recorded = new RecordedLearningSessionItem();
      recorded.setNoteTitle(entry.noteTitle());
      recorded.setScore(entry.score());
      recorded.setMemoryTrackerId(tracker.getId());
      response.getRecordedItems().add(recorded);
    }

    if (!response.getRecordedItems().isEmpty()) {
      if (!isAmend) {
        session.setStatus(LearningSessionStatus.RECORDED);
      }
      session.setRecordedAt(now);
      learningSessionRepository.save(session);
      response.setStatus(LearningSessionStatus.RECORDED);
      response.setRecordedAt(now);
    } else if (isAmend) {
      response.setStatus(LearningSessionStatus.RECORDED);
      response.setRecordedAt(session.getRecordedAt());
    } else {
      response.setStatus(LearningSessionStatus.AWAITING_REPORT);
    }

    return response;
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

  private List<MemoryTracker> dueCommissionedNoteLevelTrackers(
      User user, Notebook notebook, Timestamp now, ZoneId zoneId) {
    return userService
        .getCommissionedMemoryTrackersNeedToRepeat(user, now, zoneId)
        .filter(tracker -> tracker.getNote().getNotebook().getId().equals(notebook.getId()))
        .filter(MemoryTracker::isNoteLevelTracker)
        .toList();
  }

  private SessionItem createSessionItem(LearningSession session, MemoryTracker tracker) {
    SessionItem item = new SessionItem();
    item.setLearningSession(session);
    item.setMemoryTracker(tracker);
    item.setNoteTitle(tracker.getNote().getTitle());
    return item;
  }

  private LearningSessionCommissionResponse toCommissionResponse(
      LearningSession session, ZoneId zoneId) {
    LearningSessionCommissionResponse response = new LearningSessionCommissionResponse();
    response.setLearningSessionId(session.getId());
    response.setRequestMarkdown(learningSessionRequestMarkdownBuilder.build(session, zoneId));
    response.setStatus(session.getStatus());
    return response;
  }
}
