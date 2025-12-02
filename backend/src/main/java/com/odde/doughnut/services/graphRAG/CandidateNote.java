package com.odde.doughnut.services.graphRAG;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class CandidateNote {
  private final Note note;
  private final int depthFetched;
  private final List<RelationshipToFocusNote> discoveryPath;
  @JsonIgnore private final double relevanceScore;
  private final RelationshipToFocusNote relationshipType;

  public CandidateNote(
      Note note,
      int depthFetched,
      List<RelationshipToFocusNote> discoveryPath,
      double relevanceScore) {
    this.note = note;
    this.depthFetched = depthFetched;
    this.discoveryPath = new ArrayList<>(discoveryPath);
    this.relevanceScore = relevanceScore;
    this.relationshipType = computeRelationshipType();
  }

  private RelationshipToFocusNote computeRelationshipType() {
    return deriveRelationshipType(discoveryPath);
  }

  public static RelationshipToFocusNote deriveRelationshipType(
      List<RelationshipToFocusNote> path) {
    if (path.isEmpty()) {
      return RelationshipToFocusNote.RemotelyRelated;
    }

    // Use shortest path - if multiple paths exist, prefer higher priority
    // Priority order: Direct > One-hop > Two-hop > Three-hop+
    if (path.size() == 1) {
      return path.get(0);
    }

    // For multi-hop paths, determine relationship based on path composition
    if (path.size() == 2) {
      RelationshipToFocusNote first = path.get(0);
      RelationshipToFocusNote second = path.get(1);

      // Child -> Child = GrandChild
      if (first == RelationshipToFocusNote.Child && second == RelationshipToFocusNote.Child) {
        return RelationshipToFocusNote.GrandChild;
      }

      // Other two-hop combinations map to RemotelyRelated
      return RelationshipToFocusNote.RemotelyRelated;
    }

    // Three or more hops = RemotelyRelated
    return RelationshipToFocusNote.RemotelyRelated;
  }
}
