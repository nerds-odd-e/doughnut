package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
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
}
