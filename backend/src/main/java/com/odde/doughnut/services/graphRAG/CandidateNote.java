package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class CandidateNote {
  @Getter private final Note note;
  @Getter private final RelationshipToFocusNote relationshipType;
  @Getter private final int depthFetched;
  @Getter private final List<RelationshipToFocusNote> discoveryPath;
  @Getter private double relevanceScore;

  public CandidateNote(Note note, RelationshipToFocusNote relationshipType, int depthFetched) {
    this.note = note;
    this.relationshipType = relationshipType;
    this.depthFetched = depthFetched;
    this.discoveryPath = new ArrayList<>();
    if (relationshipType != null) {
      this.discoveryPath.add(relationshipType);
    }
  }

  public CandidateNote(
      Note note,
      RelationshipToFocusNote relationshipType,
      int depthFetched,
      List<RelationshipToFocusNote> discoveryPath) {
    this.note = note;
    this.relationshipType = relationshipType;
    this.depthFetched = depthFetched;
    this.discoveryPath = new ArrayList<>(discoveryPath);
  }

  public void setRelevanceScore(double score) {
    this.relevanceScore = score;
  }
}
