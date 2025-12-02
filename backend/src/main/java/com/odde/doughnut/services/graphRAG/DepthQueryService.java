package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
