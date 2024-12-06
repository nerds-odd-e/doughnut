package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FocusNote {
  private String uriAndTitle;
  private String details;
  private String parentUriAndTitle;
  private String objectUriAndTitle;
  private final List<String> children = new ArrayList<>();
  private final List<String> youngerSiblings = new ArrayList<>();
  private final List<String> contextualPath = new ArrayList<>();

  public static FocusNote fromNote(Note note) {
    FocusNote focusNote = new FocusNote();
    focusNote.setUriAndTitle(note.getUriAndTitle());
    focusNote.setDetails(note.getDetails());

    // Set parent and contextual path
    if (note.getParent() != null) {
      focusNote.setParentUriAndTitle(note.getParent().getUriAndTitle());
      // Add all ancestors to contextual path in order (root to parent)
      note.getAncestors()
          .forEach(ancestor -> focusNote.getContextualPath().add(ancestor.getUriAndTitle()));
    }

    if (note.getTargetNote() != null) {
      focusNote.setObjectUriAndTitle(note.getTargetNote().getUriAndTitle());
    }
    return focusNote;
  }
}
