package com.odde.doughnut.services.impl;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GraphRAGServiceImpl {
  public static final int RELATED_NOTE_DETAILS_TRUNCATE_LENGTH = 1000;

  private String formatUriAndTitle(Note note) {
    return String.format("[%s](%s)", note.getTopicConstructor(), note.getUri());
  }

  private String truncateDetails(String details) {
    if (details == null || details.length() <= RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) {
      return details;
    }
    return details.substring(0, RELATED_NOTE_DETAILS_TRUNCATE_LENGTH);
  }

  private List<String> buildContextualPath(Note note) {
    if (note.getParent() == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(formatUriAndTitle(note.getParent()));
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudget) {
    String uriAndTitle = formatUriAndTitle(focusNote);
    String detailsTruncated = focusNote.getDetails();
    String parentUriAndTitle =
        focusNote.getParent() != null ? formatUriAndTitle(focusNote.getParent()) : null;
    String objectUriAndTitle =
        focusNote.getTargetNote() != null ? formatUriAndTitle(focusNote.getTargetNote()) : null;

    FocusNote focus =
        new FocusNote(
            uriAndTitle,
            detailsTruncated,
            parentUriAndTitle,
            objectUriAndTitle,
            buildContextualPath(focusNote),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

    List<BareNote> relatedNotes = new ArrayList<>();
    if (focusNote.getTargetNote() != null) {
      Note target = focusNote.getTargetNote();
      relatedNotes.add(
          new BareNote(
              formatUriAndTitle(target), truncateDetails(target.getDetails()), null, null));
    }

    return new GraphRAGResult(focus, relatedNotes);
  }
}
