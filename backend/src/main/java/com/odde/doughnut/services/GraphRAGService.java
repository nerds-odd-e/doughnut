package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.List;

public class GraphRAGService {
  public static final int RELATED_NOTE_DETAILS_TRUNCATE_LENGTH = 500;
  public static final double CHARACTERS_PER_TOKEN = 3.75;

  private final RelationshipHandler relationshipChain;

  public GraphRAGService() {
    // Set up the chain in priority order
    ParentRelationshipHandler parentHandler = new ParentRelationshipHandler(this);
    ObjectRelationshipHandler objectHandler = new ObjectRelationshipHandler(this);
    parentHandler.setNext(objectHandler);
    this.relationshipChain = parentHandler;
  }

  private int estimateTokens(Note note) {
    int detailsLength =
        note.getDetails() != null
            ? Math.min(note.getDetails().length(), RELATED_NOTE_DETAILS_TRUNCATE_LENGTH)
            : 0;
    int titleLength = note.getTopicConstructor().length();
    return (int) Math.ceil((detailsLength + titleLength) / CHARACTERS_PER_TOKEN);
  }

  public List<BareNote> addNoteToRelatedNotes(
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

    relationshipChain.handle(focusNote, focus, relatedNotes, tokenBudgetForRelatedNotes);

    result.setFocusNote(focus);
    result.setRelatedNotes(relatedNotes);
    return result;
  }
}
