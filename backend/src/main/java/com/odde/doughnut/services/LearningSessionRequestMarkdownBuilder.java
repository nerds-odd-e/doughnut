package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LearningSessionRequestMarkdownBuilder {

  private final SessionItemRepository sessionItemRepository;

  @Autowired
  public LearningSessionRequestMarkdownBuilder(SessionItemRepository sessionItemRepository) {
    this.sessionItemRepository = sessionItemRepository;
  }

  public String build(LearningSession session, ZoneId zoneId) {
    StringBuilder sb = new StringBuilder();

    sb.append("# Learning Session Request\n\n");
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
      appendSessionItem(sb, item);
    }

    return sb.toString();
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

  private void appendSessionItem(StringBuilder sb, SessionItem item) {
    String rawContent =
        NoteContentMarkdown.bodyWithoutLeadingFrontmatter(
            item.getMemoryTracker().getNote().getContent());
    String content = rawContent == null ? "" : rawContent.trim();

    sb.append("\n### ").append(item.getNoteTitle()).append("\n");
    sb.append("- Expected learning content: ").append(content).append("\n");
    sb.append("- Learning status: not yet tutored\n");
  }
}
