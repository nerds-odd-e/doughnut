package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepthQueryService {
  public List<Note> queryDepth1ParentAndObject(Note focusNote) {
    List<Note> result = new ArrayList<>();
    if (focusNote.getParent() != null) {
      result.add(focusNote.getParent());
    }
    if (focusNote.getTargetNote() != null) {
      result.add(focusNote.getTargetNote());
    }
    return result;
  }

  public List<Note> queryDepth1InboundReferences(Note focusNote) {
    List<Note> inboundRefs = new ArrayList<>(focusNote.getInboundReferences());
    Collections.shuffle(inboundRefs);
    return inboundRefs;
  }

  public List<Note> queryDepth1Children(Note focusNote) {
    // Children are already ordered by siblingOrder via @OrderBy annotation
    return new ArrayList<>(focusNote.getChildren());
  }

  /**
   * Query depth 2 relationships from source notes (depth 1 notes).
   *
   * @param sourceNotes List of notes discovered at depth 1
   * @return Map from relationship type string to list of notes discovered via that relationship
   */
  public Map<String, List<Note>> queryDepth2FromSourceNotes(List<Note> sourceNotes) {
    Map<String, List<Note>> result = new HashMap<>();
    result.put("parent", new ArrayList<>());
    result.put("object", new ArrayList<>());
    result.put("child", new ArrayList<>());
    result.put("inbound_reference", new ArrayList<>());

    for (Note sourceNote : sourceNotes) {
      // Parents of source notes
      if (sourceNote.getParent() != null) {
        result.get("parent").add(sourceNote.getParent());
      }

      // Objects of source notes (if reification)
      if (sourceNote.getTargetNote() != null) {
        result.get("object").add(sourceNote.getTargetNote());
      }

      // Children of source notes (ordered by siblingOrder)
      result.get("child").addAll(sourceNote.getChildren());

      // Inbound references of source notes (random via shuffle)
      List<Note> inboundRefs = new ArrayList<>(sourceNote.getInboundReferences());
      Collections.shuffle(inboundRefs);
      result.get("inbound_reference").addAll(inboundRefs);
    }

    return result;
  }
}
