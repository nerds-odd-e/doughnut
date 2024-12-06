package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.List;

public class GraphRAGService {
  public static final int RELATED_NOTE_DETAILS_TRUNCATE_LENGTH = 500;
  public static final double CHARACTERS_PER_TOKEN = 3.75;

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResult result = new GraphRAGResult();
    FocusNote focus = FocusNote.fromNote(focusNote);

    // Add contextual path if parent exists
    if (focusNote.getParent() != null) {
      String parentUriAndTitle = focusNote.getParent().getUriAndTitle();
      focus.getContextualPath().add(parentUriAndTitle);

      // Add parent to related notes if budget allows
      if (tokenBudgetForRelatedNotes > 0) {
        List<BareNote> relatedNotes = new ArrayList<>();
        relatedNotes.add(BareNote.fromNote(focusNote.getParent(), RelationshipToFocusNote.Parent));
        result.setRelatedNotes(relatedNotes);
      }
    }

    result.setFocusNote(focus);
    return result;
  }
}
