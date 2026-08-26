package com.odde.donut.services.focusContext;

import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.ApproximateUtf8TokenBudget;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FocusContextRelatedNoteMaterializer {
  private FocusContextRelatedNoteMaterializer() {}

  record Materialized(FocusContextNote note, int tokenCost) {}

  static Materialized materialize(Note hydratedNote, int depth, List<String> retrievalPath) {
    String truncatedContent =
        ApproximateUtf8TokenBudget.truncateByApproxTokens(
            hydratedNote.getContent(), FocusContextConstants.RELATED_NOTE_CONTENT_MAX_TOKENS);
    boolean truncated =
        truncatedContent != null
            && hydratedNote.getContent() != null
            && truncatedContent.length() < hydratedNote.getContent().length();
    int cost = Math.max(1, ApproximateUtf8TokenBudget.estimateApproxTokens(truncatedContent));
    FocusContextNote note =
        new FocusContextNote(
            hydratedNote.getNotebook() != null ? hydratedNote.getNotebook().getName() : null,
            hydratedNote.getTitle(),
            FolderTrailSegments.crumbPathJoinedBySlashSpace(hydratedNote),
            depth,
            retrievalPath,
            hydratedNote.getCreatedAt(),
            truncatedContent,
            truncated);
    return new Materialized(note, cost);
  }

  static Map<Integer, Note> hydrateById(NoteRepository noteRepository, List<Integer> ids) {
    Map<Integer, Note> hydratedById = new LinkedHashMap<>();
    if (ids.isEmpty()) {
      return hydratedById;
    }
    for (Note n : noteRepository.hydrateNonDeletedNotesWithNotebookAndFolderByIds(ids)) {
      hydratedById.put(n.getId(), n);
    }
    return hydratedById;
  }
}
