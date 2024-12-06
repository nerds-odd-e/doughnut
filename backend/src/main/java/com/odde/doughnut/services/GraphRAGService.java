package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.List;

public class GraphRAGService {
  public static final int RELATED_NOTE_DETAILS_TRUNCATE_LENGTH = 500;
  public static final double CHARACTERS_PER_TOKEN = 3.75;

  private int estimateTokens(Note note) {
    int detailsLength =
        note.getDetails() != null
            ? Math.min(note.getDetails().length(), RELATED_NOTE_DETAILS_TRUNCATE_LENGTH)
            : 0;
    int titleLength = note.getTopicConstructor().length();
    return (int) Math.ceil((detailsLength + titleLength) / CHARACTERS_PER_TOKEN);
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResult result = new GraphRAGResult();
    FocusNote focus = FocusNote.fromNote(focusNote);

    // Add contextual path if parent exists
    if (focusNote.getParent() != null) {
      String parentUriAndTitle = focusNote.getParent().getUriAndTitle();
      focus.getContextualPath().add(parentUriAndTitle);

      // Add parent to related notes only if budget is enough
      int parentTokens = estimateTokens(focusNote.getParent());
      if (tokenBudgetForRelatedNotes >= parentTokens) {
        List<BareNote> relatedNotes = new ArrayList<>();
        relatedNotes.add(BareNote.fromNote(focusNote.getParent(), RelationshipToFocusNote.Parent));
        result.setRelatedNotes(relatedNotes);
      }
    }

    result.setFocusNote(focus);
    return result;
  }
}
