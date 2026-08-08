package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.LearningSessionCommissionResponse;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.LearningSessionRepository;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
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

  @Autowired
  public LearningSessionService(
      UserService userService,
      LearningSessionRepository learningSessionRepository,
      SessionItemRepository sessionItemRepository,
      LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder) {
    this.userService = userService;
    this.learningSessionRepository = learningSessionRepository;
    this.sessionItemRepository = sessionItemRepository;
    this.learningSessionRequestMarkdownBuilder = learningSessionRequestMarkdownBuilder;
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
