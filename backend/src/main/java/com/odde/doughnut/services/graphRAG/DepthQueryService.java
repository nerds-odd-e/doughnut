package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.ArrayList;
import java.util.List;

public class DepthQueryService {
  public DepthQueryService() {}

  public List<DepthQueryResult> fetchDepth1Candidates(Note focusNote) {
    List<DepthQueryResult> results = new ArrayList<>();

    // Parent
    if (focusNote.getParent() != null && focusNote.getParent().getDeletedAt() == null) {
      results.add(
          new DepthQueryResult(
              focusNote.getParent(), RelationshipToFocusNote.Parent, focusNote));
    }

    // Object
    if (focusNote.getTargetNote() != null
        && focusNote.getTargetNote().getDeletedAt() == null) {
      results.add(
          new DepthQueryResult(
              focusNote.getTargetNote(), RelationshipToFocusNote.Object, focusNote));
    }

    // Children
    for (Note child : focusNote.getChildren()) {
      if (child.getDeletedAt() == null) {
        results.add(
            new DepthQueryResult(child, RelationshipToFocusNote.Child, focusNote));
      }
    }

    // Inbound references
    for (Note inbound : focusNote.getInboundReferences()) {
      if (inbound.getDeletedAt() == null) {
        results.add(
            new DepthQueryResult(
                inbound, RelationshipToFocusNote.InboundReference, focusNote));
      }
    }

    return results;
  }

  public List<DepthQueryResult> fetchDepthNCandidates(List<Note> sourceNotes, int depth) {
    if (sourceNotes.isEmpty()) {
      return new ArrayList<>();
    }

    List<DepthQueryResult> results = new ArrayList<>();

    // Batch process if source notes > 100
    int batchSize = 100;
    for (int i = 0; i < sourceNotes.size(); i += batchSize) {
      int end = Math.min(i + batchSize, sourceNotes.size());
      List<Note> batch = sourceNotes.subList(i, end);
      results.addAll(fetchDepthNBatch(batch, depth));
    }

    return results;
  }

  private List<DepthQueryResult> fetchDepthNBatch(List<Note> sourceNotes, int depth) {
    List<DepthQueryResult> results = new ArrayList<>();

    for (Note sourceNote : sourceNotes) {
      if (sourceNote.getDeletedAt() != null) {
        continue;
      }

      // Children
      for (Note child : sourceNote.getChildren()) {
        if (child.getDeletedAt() == null) {
          results.add(
              new DepthQueryResult(child, RelationshipToFocusNote.Child, sourceNote));
        }
      }

      // Inbound references
      for (Note inbound : sourceNote.getInboundReferences()) {
        if (inbound.getDeletedAt() == null) {
          results.add(
              new DepthQueryResult(
                  inbound, RelationshipToFocusNote.InboundReference, sourceNote));
        }
      }
    }

    return results;
  }
}
