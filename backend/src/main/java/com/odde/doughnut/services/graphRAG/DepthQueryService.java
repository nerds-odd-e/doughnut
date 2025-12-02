package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.ArrayList;
import java.util.List;

public class DepthQueryService {

  public List<DepthQueryResult> fetchDepth1Candidates(Note focusNote) {
    List<DepthQueryResult> results = new ArrayList<>();

    if (focusNote.getParent() != null) {
      results.add(
          new DepthQueryResult(
              focusNote.getParent(), RelationshipToFocusNote.Parent, focusNote));
    }

    if (focusNote.getTargetNote() != null) {
      results.add(
          new DepthQueryResult(
              focusNote.getTargetNote(), RelationshipToFocusNote.Object, focusNote));
    }

    for (Note child : focusNote.getChildren()) {
      results.add(new DepthQueryResult(child, RelationshipToFocusNote.Child, focusNote));
    }

    for (Note inboundRef : focusNote.getInboundReferences()) {
      results.add(
          new DepthQueryResult(
              inboundRef, RelationshipToFocusNote.InboundReference, focusNote));
    }

    return results;
  }

  public List<DepthQueryResult> fetchDepthNCandidates(List<Note> sourceNotes, int depth) {
    List<DepthQueryResult> results = new ArrayList<>();

    for (Note sourceNote : sourceNotes) {
      if (sourceNote.getParent() != null) {
        results.add(
            new DepthQueryResult(
                sourceNote.getParent(), RelationshipToFocusNote.Parent, sourceNote));
      }

      if (sourceNote.getTargetNote() != null) {
        results.add(
            new DepthQueryResult(
                sourceNote.getTargetNote(), RelationshipToFocusNote.Object, sourceNote));
      }

      for (Note child : sourceNote.getChildren()) {
        results.add(new DepthQueryResult(child, RelationshipToFocusNote.Child, sourceNote));
      }

      for (Note inboundRef : sourceNote.getInboundReferences()) {
        results.add(
            new DepthQueryResult(
                inboundRef, RelationshipToFocusNote.InboundReference, sourceNote));
      }
    }

    return results;
  }
}

