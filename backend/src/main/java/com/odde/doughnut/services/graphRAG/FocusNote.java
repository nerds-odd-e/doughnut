package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class FocusNote extends BareNote {
  private final List<String> contextualPath = new ArrayList<>();
  private final List<String> children = new ArrayList<>();
  private final List<String> priorSiblings = new ArrayList<>();
  private final List<String> youngerSiblings = new ArrayList<>();
  private final List<String> inboundReferences = new ArrayList<>();

  private FocusNote(Note note) {
    super(note, note.getDetails(), RelationshipToFocusNote.Self);

    // Add contextual path (unique to FocusNote)
    if (note.getParent() != null) {
      note.getAncestors().forEach(ancestor -> contextualPath.add(ancestor.getUri()));
    }
  }

  public static FocusNote fromNote(Note note) {
    return new FocusNote(note);
  }
}
