package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphRAGResultBuilder {
  private int remainingBudget;
  private final Map<Note, BareNote> addedNotes = new HashMap<>();
  private final List<BareNote> relatedNotes = new ArrayList<>();
  private final FocusNote focus;

  public GraphRAGResultBuilder(Note focusNote, int tokenBudget) {
    this.remainingBudget = tokenBudget;
    this.focus = FocusNote.fromNote(focusNote);
  }

  public BareNote addNoteToRelatedNotes(Note note, RelationshipToFocusNote relationship) {
    // Check if note was already added with a higher priority relationship
    BareNote existingNote = addedNotes.get(note);
    if (existingNote != null) {
      // Note was already added, don't add it again
      return existingNote;
    }

    int tokens = estimateTokens(note);
    if (tokens <= remainingBudget) {
      BareNote bareNote = BareNote.fromNote(note, relationship);
      relatedNotes.add(bareNote);
      remainingBudget -= tokens;
      addedNotes.put(note, bareNote);
      return bareNote;
    }
    return null;
  }

  private int estimateTokens(Note note) {
    int detailsLength =
        note.getDetails() != null
            ? Math.min(
                note.getDetails().length(), GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH)
            : 0;
    int titleLength = note.getTopicConstructor().length();
    return (int) Math.ceil((detailsLength + titleLength) / GraphRAGConstants.CHARACTERS_PER_TOKEN);
  }

  public FocusNote getFocusNote() {
    return focus;
  }

  public GraphRAGResult build() {
    GraphRAGResult result = new GraphRAGResult();
    result.setFocusNote(focus);
    result.setRelatedNotes(relatedNotes);
    return result;
  }
}
