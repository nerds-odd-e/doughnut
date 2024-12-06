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

  private List<BareNote> addNoteToRelatedNotes(
      List<BareNote> relatedNotes, Note note, RelationshipToFocusNote relationship, int budget) {
    int tokens = estimateTokens(note);
    if (tokens <= budget) {
      relatedNotes.add(BareNote.fromNote(note, relationship));
    }
    return relatedNotes;
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResult result = new GraphRAGResult();
    FocusNote focus = FocusNote.fromNote(focusNote);
    List<BareNote> relatedNotes = new ArrayList<>();

    // Add contextual path if parent exists
    if (focusNote.getParent() != null) {
      String parentUriAndTitle = focusNote.getParent().getUriAndTitle();
      focus.getContextualPath().add(parentUriAndTitle);

      // Add parent to related notes if budget allows
      addNoteToRelatedNotes(
          relatedNotes,
          focusNote.getParent(),
          RelationshipToFocusNote.Parent,
          tokenBudgetForRelatedNotes);
    }

    // Add target/object note if exists
    if (focusNote.getTargetNote() != null) {
      addNoteToRelatedNotes(
          relatedNotes,
          focusNote.getTargetNote(),
          RelationshipToFocusNote.Object,
          tokenBudgetForRelatedNotes);
    }

    result.setFocusNote(focus);
    result.setRelatedNotes(relatedNotes);
    return result;
  }
}
