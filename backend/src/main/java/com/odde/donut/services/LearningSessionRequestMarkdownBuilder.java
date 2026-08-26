package com.odde.donut.services;

import com.odde.donut.algorithms.FrontmatterQuestionGenerationInstruction;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.RecallLogRepository;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextResult;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.focusContext.MergedRelatedNotes;
import com.odde.donut.services.focusContext.RetrievalConfig;
import java.sql.Timestamp;
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
    String relatedNotes =
        focusContextMarkdownRenderer.renderRelatedNotes(
            mergedRelatedNotes.asList(), config.getMaxDepth());
    sb.append(relatedNotes);
    if (!relatedNotes.isEmpty()) {
      sb.append("\n");
    }
  }

  private void appendHowToReport(StringBuilder sb, List<MemoryTracker> trackers) {
    sb.append("<how_to_report>\n");
    sb.append("Teach the session items above, then return a Learning Session Report giving one\n");
    sb.append("Grade from 1 to 4 and descriptive text per item:\n\n");
    sb.append("- 4 — mastered the session item with full fluency\n");
    sb.append("- 3 — mastered the session item with fluency\n");
    sb.append(
        "- 2 — mastered the session item but not fluent, or needed a reminder then showed mastery\n");
    sb.append(
        "- 1 — needed several reminders, or could not reach the session item even with help\n\n");
    sb.append("Preferred report format: one `###` heading per item, then `Grade: N`, then\n");
    sb.append("descriptive text until the next heading or the close tag.\n\n");
    sb.append("Example of how to provide feedback:\n\n");
    sb.append("# Learning Session Report\n\n");
    sb.append(LearningSessionReportParser.SESSION_ITEM_FEEDBACK_OPEN_TAG).append("\n");
    appendExampleReportFeedback(sb, trackers);
    sb.append("\n").append(LearningSessionReportParser.SESSION_ITEM_FEEDBACK_CLOSE_TAG);
    sb.append(
        "\n\nOnly grade session items that were actually taught in this session. Do not list\n");
    sb.append("items that were not taught in the session.\n");
    sb.append("</how_to_report>\n");
  }

  private void appendExampleReportFeedback(StringBuilder sb, List<MemoryTracker> trackers) {
    if (trackers.isEmpty()) {
      return;
    }
    appendExampleFeedbackItem(
        sb,
        trackers.getFirst().getNote().getTitle(),
        4,
        "Pronunciation was clear; still mixes ser/estar under pressure.");
    if (trackers.size() > 1) {
      sb.append("\n\n");
      appendExampleFeedbackItem(
          sb, trackers.get(1).getNote().getTitle(), 1, "Needed several reminders on the soft g.");
    }
  }

  private void appendExampleFeedbackItem(
      StringBuilder sb, String title, int grade, String descriptiveText) {
    sb.append("### ").append(title).append("\n");
    sb.append("Grade: ").append(grade).append("\n");
    sb.append(descriptiveText);
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
    List<RecallLog> tutorLogs = recallLogRepository.findTutorLogsByMemoryTrackerId(tracker.getId());
    sb.append("- Tutoring status: ").append(tutoringStatusLine(tutorLogs, zoneId)).append("\n");
    appendRecentFeedbacks(sb, tutorLogs, zoneId);
    FocusContextResult focusContextResult =
        focusContextRetrievalService.retrieve(note, viewer, config);
    sb.append(focusContextMarkdownRenderer.renderFocusNote(focusContextResult.getFocusNote()));
    mergedRelatedNotes.addAll(focusContextResult.getRelatedNotes());
  }

  private void appendRecentFeedbacks(StringBuilder sb, List<RecallLog> tutorLogs, ZoneId zoneId) {
    for (RecallLog log : tutorLogs.stream().limit(2).toList().reversed()) {
      appendDatedFeedback(sb, log, zoneId);
    }
  }

  private void appendDatedFeedback(StringBuilder sb, RecallLog log, ZoneId zoneId) {
    sb.append("- ")
        .append(isoDate(log.getRecordedAt(), zoneId))
        .append(" — Grade: ")
        .append(log.getGrade().getValue())
        .append("\n");
    String descriptiveText = log.getTutorFeedback();
    if (descriptiveText != null && !descriptiveText.isBlank()) {
      sb.append("  ").append(descriptiveText).append("\n");
    }
  }

  private String tutoringStatusLine(List<RecallLog> tutorLogs, ZoneId zoneId) {
    if (tutorLogs.isEmpty()) {
      return "not yet tutored";
    }
    String sessionWord = tutorLogs.size() == 1 ? "session" : "sessions";
    return tutorLogs.size()
        + " previous "
        + sessionWord
        + ", last on "
        + isoDate(tutorLogs.getFirst().getRecordedAt(), zoneId);
  }

  private static String isoDate(Timestamp recordedAt, ZoneId zoneId) {
    return recordedAt.toInstant().atZone(zoneId).toLocalDate().format(ISO_DATE);
  }
}
