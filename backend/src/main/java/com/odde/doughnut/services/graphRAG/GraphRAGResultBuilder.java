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
  private final TokenCountingStrategy tokenCountingStrategy;

  public GraphRAGResultBuilder(
      Note focusNote, int tokenBudget, TokenCountingStrategy tokenCountingStrategy) {
    this.remainingBudget = tokenBudget;
    this.focus = FocusNote.fromNote(focusNote);
    this.tokenCountingStrategy = tokenCountingStrategy;
  }

  public BareNote addNoteToRelatedNotes(Note note, RelationshipToFocusNote relationship) {
    BareNote existingNote = addedNotes.get(note);
    if (existingNote != null) {
      return existingNote;
    }

    int tokens = tokenCountingStrategy.estimateTokens(note);
    if (tokens <= remainingBudget) {
      BareNote bareNote = BareNote.fromNote(note, relationship);
      relatedNotes.add(bareNote);
      remainingBudget -= tokens;
      addedNotes.put(note, bareNote);
      return bareNote;
    }
    return null;
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
