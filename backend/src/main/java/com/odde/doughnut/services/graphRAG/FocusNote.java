package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FocusNote extends BareNote {
  private final List<UriAndTitle> children = new ArrayList<>();
  private final List<UriAndTitle> priorSiblings = new ArrayList<>();
  private final List<UriAndTitle> youngerSiblings = new ArrayList<>();
  private final List<UriAndTitle> contextualPath = new ArrayList<>();
  private final List<UriAndTitle> referrings = new ArrayList<>();

  public static FocusNote fromNote(Note note) {
    FocusNote focusNote = new FocusNote();
    initializeFromNote(focusNote, note, RelationshipToFocusNote.Self);

    // Add contextual path (unique to FocusNote)
    if (note.getParent() != null) {
      note.getAncestors()
          .forEach(ancestor -> focusNote.getContextualPath().add(UriAndTitle.fromNote(ancestor)));
    }

    return focusNote;
  }
}
