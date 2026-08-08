package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.FrontmatterQuestionGenerationInstruction;
import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.entities.repositories.RecordedFeedbackSummary;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LearningSessionRequestMarkdownBuilder {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private final SessionItemRepository sessionItemRepository;

  @Autowired
  public LearningSessionRequestMarkdownBuilder(SessionItemRepository sessionItemRepository) {
    this.sessionItemRepository = sessionItemRepository;
  }

  public String build(LearningSession session, ZoneId zoneId) {
    StringBuilder sb = new StringBuilder();

    sb.append("# Learning Session Request\n\n");
    appendInstructions(sb, session);
    sb.append("Notebook: ").append(session.getNotebook().getName()).append("\n\n");

    appendRubric(sb);

    sb.append("\n## Session Items\n");

    List<SessionItem> items =
        sessionItemRepository.findByLearningSession_Id(session.getId()).stream()
            .sorted(
                Comparator.comparing(
                    item -> item.getMemoryTracker().getNextRecallAt(),
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    for (SessionItem item : items) {
      appendSessionItem(sb, item, zoneId);
    }

    return sb.toString();
  }

  private void appendInstructions(StringBuilder sb, LearningSession session) {
    sb.append("## Instructions\n\n");
    sb.append("You are the tutor to help the learner to study ")
        .append(session.getNotebook().getName())
        .append(".\n\n");
    FrontmatterQuestionGenerationInstruction.fromNoteContent(
            session.getNotebook().getReadmeContent())
        .ifPresent(instruction -> sb.append(instruction).append("\n\n"));
    sb.append("Wait for the learner's instruction before starting the learning session.\n\n");
  }

  private void appendRubric(StringBuilder sb) {
    sb.append("## How to report\n\n");
    sb.append("Teach the session items below, then return a Learning Session Report giving one\n");
    sb.append("score from 0 to 5 per item:\n\n");
    sb.append("- 5 — mastered the learning point with full fluency\n");
    sb.append("- 4 — mastered the learning point with fluency\n");
    sb.append("- 3 — mastered the learning point, but not fluent\n");
    sb.append("- 2 — needed a reminder at first, then showed signs of mastering it\n");
    sb.append("- 1 — needed several reminders\n");
    sb.append("- 0 — could not reach the learning point even with help\n");
  }

  private void appendSessionItem(StringBuilder sb, SessionItem item, ZoneId zoneId) {
    String rawContent =
        NoteContentMarkdown.bodyWithoutLeadingFrontmatter(
            item.getMemoryTracker().getNote().getContent());
    String content = rawContent == null ? "" : rawContent.trim();

    sb.append("\n### ").append(item.getNoteTitle()).append("\n");
    sb.append("- Expected learning content: ").append(content).append("\n");
    sb.append("- Learning status: ")
        .append(learningStatusLine(item.getMemoryTracker().getId(), zoneId))
        .append("\n");
  }

  private String learningStatusLine(Integer memoryTrackerId, ZoneId zoneId) {
    RecordedFeedbackSummary summary =
        sessionItemRepository.summarizeRecordedFeedbackByMemoryTrackerId(memoryTrackerId);
    if (summary.sessionCount() == 0) {
      return "not yet tutored";
    }

    String lastDate =
        summary.lastRecordedAt().toInstant().atZone(zoneId).toLocalDate().format(ISO_DATE);
    String sessionWord = summary.sessionCount() == 1 ? "session" : "sessions";
    return summary.sessionCount() + " previous " + sessionWord + ", last on " + lastDate;
  }
}
