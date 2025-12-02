package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import lombok.Getter;

public class CandidateNote {
  @Getter private final Note note;
  @Getter private final RelationshipToFocusNote relationshipType;
  @Getter private final int depthFetched;
  @Getter private double relevanceScore;

  public CandidateNote(Note note, RelationshipToFocusNote relationshipType, int depthFetched) {
    this.note = note;
    this.relationshipType = relationshipType;
    this.depthFetched = depthFetched;
  }

  public void setRelevanceScore(double score) {
    this.relevanceScore = score;
  }
}
