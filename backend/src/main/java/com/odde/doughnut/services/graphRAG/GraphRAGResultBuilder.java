package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.HashMap;
import java.util.Map;

public class GraphRAGResultBuilder {
  private int remainingBudget;
  private final Map<Note, BareNote> addedNotes = new HashMap<>();
  private final GraphRAGResult result;
  private final TokenCountingStrategy tokenCountingStrategy;

  public GraphRAGResultBuilder(
      Note focusNote, int tokenBudget, TokenCountingStrategy tokenCountingStrategy) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    FocusNote focus = FocusNote.fromNote(focusNote);
    this.result = new GraphRAGResult(focus);
    this.remainingBudget = tokenBudget - tokenCountingStrategy.estimateTokens(focus);
  }

  public BareNote addNoteToRelatedNotes(Note note, RelationshipToFocusNote relationship) {
    BareNote existingNote = addedNotes.get(note);
    if (existingNote != null) {
      return existingNote;
    }

    BareNote bareNote = BareNote.fromNote(note, relationship);
    int tokens = tokenCountingStrategy.estimateTokens(bareNote);
    if (tokens <= remainingBudget) {
      result.getRelatedNotes().add(bareNote);
      remainingBudget -= tokens;
      addedNotes.put(note, bareNote);
      return bareNote;
    }
    return null;
  }

  public FocusNote getFocusNote() {
    return result.getFocusNote();
  }

  public GraphRAGResult build() {
    return result;
  }
}
