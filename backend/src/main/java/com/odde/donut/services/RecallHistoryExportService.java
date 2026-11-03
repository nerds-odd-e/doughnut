package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallHistoryExport;
import com.odde.donut.controllers.dto.RecallHistoryExport.EventKind;
import com.odde.donut.entities.DailyProbe;
import com.odde.donut.entities.GradeOrConfusionConverter;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.DailyProbeRepository;
import com.odde.donut.entities.repositories.RecallLogRepository;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class RecallHistoryExportService {
  private final RecallLogRepository recallLogRepository;
  private final DailyProbeRepository dailyProbeRepository;

  public RecallHistoryExportService(
      RecallLogRepository recallLogRepository, DailyProbeRepository dailyProbeRepository) {
    this.recallLogRepository = recallLogRepository;
    this.dailyProbeRepository = dailyProbeRepository;
  }

  public RecallHistoryExport export(User user, ZoneId timeZone, Timestamp exportedAt) {
    return new RecallHistoryExport(
        timeZone.getId(),
        format(exportedAt, timeZone),
        recallLogRepository.findRecallEventRows(user.getId()).stream()
            .map(row -> event(row, timeZone))
            .toList(),
        dailyProbeRepository.findByUserOrderByCompletedAtAscIdAsc(user).stream()
            .map(probe -> dailyProbe(probe, timeZone))
            .toList());
  }

  private static RecallHistoryExport.DailyProbe dailyProbe(DailyProbe probe, ZoneId zone) {
    return new RecallHistoryExport.DailyProbe(
        format(probe.getCompletedAt(), zone),
        probe.getSpeed(),
        probe.getAccuracy(),
        probe.getLapseCount(),
        probe.getVariability(),
        probe.getTrials());
  }

  private static RecallHistoryExport.Event event(RecallEventRow row, ZoneId zone) {
    return new RecallHistoryExport.Event(
        format(row.recordedAt(), zone),
        GradeOrConfusionConverter.productOutcomeOf(row.grade()),
        kind(row),
        row.tutorFeedback(),
        row.elapsedHours(),
        row.stabilityBefore(),
        row.difficultyBefore(),
        row.retrievability(),
        row.memoryTrackerId(),
        row.memoryTrackerType(),
        row.noteId(),
        row.answerId() == null ? null : answer(row, zone));
  }

  private static EventKind kind(RecallEventRow row) {
    if (row.answerId() != null) return EventKind.ANSWERED_RECALL_PROMPT;
    if (row.tutorFeedback() != null) return EventKind.TUTOR_FEEDBACK;
    return EventKind.JUST_REVIEW;
  }

  private static RecallHistoryExport.Answer answer(RecallEventRow row, ZoneId zone) {
    return new RecallHistoryExport.Answer(
        row.answerId(),
        row.questionType(),
        format(row.promptCreatedAt(), zone),
        format(row.answerCreatedAt(), zone),
        row.thinkingTimeMs(),
        row.awayMs(),
        row.awayCount(),
        row.detourMs(),
        row.detourCount(),
        row.idleMs(),
        row.outcome(),
        format(row.excludedAt(), zone),
        row.exclusionReason());
  }

  private static String format(Timestamp timestamp, ZoneId zone) {
    return timestamp == null
        ? null
        : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp.toInstant().atZone(zone));
  }
}
