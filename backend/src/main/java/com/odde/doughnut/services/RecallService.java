package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AwaitingReportLearningSessionLite;
import com.odde.doughnut.controllers.dto.DueCommissionedMemoryTrackerLite;
import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.MemoryTrackerLite;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.LearningSessionRepository;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecallService {

  private final UserService userService;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final SessionItemRepository sessionItemRepository;
  private final LearningSessionRepository learningSessionRepository;
  private final LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder;

  @Autowired
  public RecallService(
      UserService userService,
      MemoryTrackerRepository memoryTrackerRepository,
      RecallPromptRepository recallPromptRepository,
      SessionItemRepository sessionItemRepository,
      LearningSessionRepository learningSessionRepository,
      LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder) {
    this.userService = userService;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.sessionItemRepository = sessionItemRepository;
    this.learningSessionRepository = learningSessionRepository;
    this.learningSessionRequestMarkdownBuilder = learningSessionRequestMarkdownBuilder;
  }

  private int totalAssimilatedCount(User user) {
    return memoryTrackerRepository.countByUserNotRemoved(user.getId());
  }

  private Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone, int dueInDays) {
    return userService.getMemoryTrackersNeedToRepeat(
        user,
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24),
        timeZone);
  }

  private Stream<MemoryTracker> getCommissionedMemoryTrackersNeedToRepeat(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone, int dueInDays) {
    return userService.getCommissionedMemoryTrackersNeedToRepeat(
        user,
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24),
        timeZone);
  }

  public DueMemoryTrackers getDueMemoryTrackers(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone, int dueInDays) {
    List<MemoryTrackerLite> toRepeat =
        getMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, dueInDays)
            .map(
                mt -> {
                  MemoryTrackerLite lite = new MemoryTrackerLite();
                  lite.setMemoryTrackerId(mt.getId());
                  lite.setSpelling(mt.isSpelling());
                  String propertyKey = mt.getPropertyKey();
                  lite.setPropertyKey(
                      propertyKey == null || propertyKey.isEmpty() ? null : propertyKey);
                  return lite;
                })
            .toList();
    Set<Integer> awaitingReportTrackerIds =
        new HashSet<>(
            sessionItemRepository.findMemoryTrackerIdsInAwaitingReportSessions(user.getId()));
    List<DueCommissionedMemoryTrackerLite> dueCommissioned =
        getCommissionedMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, dueInDays)
            .filter(mt -> !awaitingReportTrackerIds.contains(mt.getId()))
            .map(
                mt -> {
                  DueCommissionedMemoryTrackerLite lite = new DueCommissionedMemoryTrackerLite();
                  lite.setMemoryTrackerId(mt.getId());
                  Notebook notebook = mt.getNote().getNotebook();
                  lite.setNotebookId(notebook.getId());
                  lite.setNotebookName(notebook.getName());
                  return lite;
                })
            .toList();
    DueMemoryTrackers dueMemoryTrackers = new DueMemoryTrackers();
    dueMemoryTrackers.setDueInDays(dueInDays);
    dueMemoryTrackers.setToRepeat(toRepeat);
    dueMemoryTrackers.setDueCommissioned(dueCommissioned);
    dueMemoryTrackers.setAwaitingReportSessions(
        learningSessionRepository
            .findByUser_IdAndStatus(user.getId(), LearningSessionStatus.AWAITING_REPORT)
            .stream()
            .map(session -> toAwaitingReportLite(session, timeZone))
            .toList());

    // Set recall status
    dueMemoryTrackers.totalAssimilatedCount = totalAssimilatedCount(user);
    dueMemoryTrackers.setCurrentRecallWindowEndAt(
        TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone));

    return dueMemoryTrackers;
  }

  private AwaitingReportLearningSessionLite toAwaitingReportLite(
      LearningSession session, ZoneId zoneId) {
    AwaitingReportLearningSessionLite lite = new AwaitingReportLearningSessionLite();
    lite.setNotebookId(session.getNotebook().getId());
    lite.setNotebookName(session.getNotebook().getName());
    lite.setLearningSessionId(session.getId());
    lite.setRequestMarkdown(learningSessionRequestMarkdownBuilder.build(session, zoneId));
    return lite;
  }

  public int getToRecallCount(User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    return (int) getMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, 0).count();
  }

  public List<RecallPrompt> getPreviouslyAnsweredRecallPrompts(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    Timestamp startTime = TimestampOperations.startOfHalfADay(currentUTCTimestamp, timeZone);
    Timestamp endTime = TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone);

    return recallPromptRepository.findAnsweredRecallPromptsInTimeRange(
        user.getId(), startTime, endTime);
  }
}
