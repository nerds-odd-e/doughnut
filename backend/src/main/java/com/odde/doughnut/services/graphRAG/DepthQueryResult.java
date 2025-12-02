package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import lombok.Getter;

@Getter
public class DepthQueryResult {
  private final Note note;
  private final RelationshipToFocusNote relationshipType;
  private final Note sourceNote;

  public DepthQueryResult(Note note, RelationshipToFocusNote relationshipType, Note sourceNote) {
    this.note = note;
    this.relationshipType = relationshipType;
    this.sourceNote = sourceNote;
  }
}
