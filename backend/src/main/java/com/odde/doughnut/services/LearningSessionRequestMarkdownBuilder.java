package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.FrontmatterQuestionGenerationInstruction;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.RecallLogRepository;
import com.odde.doughnut.entities.repositories.TutorLogSummary;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.MergedRelatedNotes;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LearningSessionRequestMarkdownBuilder {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private final RecallLogRepository recallLogRepository;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  @Autowired
  public LearningSessionRequestMarkdownBuilder(
      RecallLogRepository recallLogRepository,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer) {
    this.recallLogRepository = recallLogRepository;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
  }

  public String build(User user, Notebook notebook, List<MemoryTracker> trackers, ZoneId zoneId) {
    List<MemoryTracker> orderedTrackers =
        trackers.stream()
            .sorted(
                Comparator.comparing(
                    MemoryTracker::getNextRecallAt,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    StringBuilder sb = new StringBuilder();
    sb.append("# Learning Session Request\n\n");
    appendInstructions(sb, notebook);
    appendSessionItemTitles(sb, orderedTrackers);
    appendSessionItems(sb, user, orderedTrackers, zoneId);
    appendHowToReport(sb, orderedTrackers);
    return sb.toString();
  }

  private void appendInstructions(StringBuilder sb, Notebook notebook) {
    sb.append("<instructions>\n");
    sb.append("You are the tutor to help the learner to study ")
        .append(notebook.getName())
        .append(".\n\n");
    FrontmatterQuestionGenerationInstruction.fromNoteContent(notebook.getReadmeContent())
        .ifPresent(instruction -> sb.append(instruction).append("\n\n"));
    sb.append("Wait for the learner's instruction before starting the learning session.\n");
    sb.append("</instructions>\n\n");
  }

  private void appendSessionItemTitles(StringBuilder sb, List<MemoryTracker> trackers) {
    sb.append("<session_item_titles>\n");
    for (MemoryTracker tracker : trackers) {
      sb.append("- ").append(tracker.getNote().getTitle()).append("\n");
    }
    sb.append("</session_item_titles>\n\n");
  }

  private void appendSessionItems(
      StringBuilder sb, User viewer, List<MemoryTracker> trackers, ZoneId zoneId) {
    RetrievalConfig config = RetrievalConfig.defaultMaxDepth();
    MergedRelatedNotes mergedRelatedNotes = new MergedRelatedNotes();
    for (MemoryTracker tracker : trackers) {
      Note note = tracker.getNote();
      mergedRelatedNotes.exclude(note.getNotebook().getName(), note.getTitle());
    }
    sb.append("<session_items>\n");
    for (MemoryTracker tracker : trackers) {
      appendSessionItem(sb, viewer, tracker, zoneId, config, mergedRelatedNotes);
    }
    sb.append("</session_items>\n\n");
    sb.append(
        focusContextMarkdownRenderer.renderRelatedNotes(
            mergedRelatedNotes.asList(), config.getMaxDepth()));
  }

  private void appendHowToReport(StringBuilder sb, List<MemoryTracker> trackers) {
    sb.append("<how_to_report>\n");
    sb.append("Teach the session items above, then return a Learning Session Report giving one\n");
    sb.append("Grade from 1 to 4 per item:\n\n");
    sb.append("- 4 — mastered the session item with full fluency\n");
    sb.append("- 3 — mastered the session item with fluency\n");
    sb.append(
        "- 2 — mastered the session item but not fluent, or needed a reminder then showed mastery\n");
    sb.append(
        "- 1 — needed several reminders, or could not reach the session item even with help\n\n");
    sb.append("Example of how to provide feedback:\n\n");
    sb.append("# Learning Session Report\n\n");
    sb.append(LearningSessionReportParser.SESSION_ITEM_GRADES_OPEN_TAG).append("\n");
    appendExampleReportGrades(sb, trackers);
    sb.append("\n").append(LearningSessionReportParser.SESSION_ITEM_GRADES_CLOSE_TAG);
    sb.append(
        "\n\nOnly grade session items that were actually taught in this session. Do not list\n");
    sb.append("items that were not taught in the session.\n");
    sb.append("</how_to_report>\n");
  }

  private void appendExampleReportGrades(StringBuilder sb, List<MemoryTracker> trackers) {
    if (trackers.isEmpty()) {
      return;
    }
    sb.append(trackers.getFirst().getNote().getTitle()).append(": 4");
    if (trackers.size() > 1) {
      sb.append("\n").append(trackers.get(1).getNote().getTitle()).append(": 1");
    }
  }

  private void appendSessionItem(
      StringBuilder sb,
      User viewer,
      MemoryTracker tracker,
      ZoneId zoneId,
      RetrievalConfig config,
      MergedRelatedNotes mergedRelatedNotes) {
    Note note = tracker.getNote();

    sb.append("### ").append(note.getTitle()).append("\n");
    sb.append("- Tutoring status: ")
        .append(tutoringStatusLine(tracker.getId(), zoneId))
        .append("\n");
    FocusContextResult focusContextResult =
        focusContextRetrievalService.retrieve(note, viewer, config);
    sb.append(focusContextMarkdownRenderer.renderFocusNote(focusContextResult.getFocusNote()));
    mergedRelatedNotes.addAll(focusContextResult.getRelatedNotes());
  }

  private String tutoringStatusLine(Integer memoryTrackerId, ZoneId zoneId) {
    TutorLogSummary summary =
        recallLogRepository.summarizeTutorLogsByMemoryTrackerId(memoryTrackerId);
    if (summary.logCount() == 0) {
      return "not yet tutored";
    }

    String lastDate =
        summary.lastRecordedAt().toInstant().atZone(zoneId).toLocalDate().format(ISO_DATE);
    String sessionWord = summary.logCount() == 1 ? "session" : "sessions";
    return summary.logCount() + " previous " + sessionWord + ", last on " + lastDate;
  }
}
