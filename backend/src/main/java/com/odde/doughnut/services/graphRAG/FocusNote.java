package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FocusNote {
  private UriAndTitle uriAndTitle;
  private String details;
  private UriAndTitle parentUriAndTitle;
  private UriAndTitle objectUriAndTitle;
  private final List<UriAndTitle> children = new ArrayList<>();
  private final List<UriAndTitle> priorSiblings = new ArrayList<>();
  private final List<UriAndTitle> youngerSiblings = new ArrayList<>();
  private final List<UriAndTitle> contextualPath = new ArrayList<>();
  private final List<UriAndTitle> referrings = new ArrayList<>();

  public static FocusNote fromNote(Note note) {
    FocusNote focusNote = new FocusNote();
    focusNote.setUriAndTitle(UriAndTitle.fromNote(note));
    focusNote.setDetails(note.getDetails());

    // Set parent and contextual path
    if (note.getParent() != null) {
      focusNote.setParentUriAndTitle(UriAndTitle.fromNote(note.getParent()));
      // Add all ancestors to contextual path in order (root to parent)
      note.getAncestors()
          .forEach(ancestor -> focusNote.getContextualPath().add(UriAndTitle.fromNote(ancestor)));
    }

    if (note.getTargetNote() != null) {
      focusNote.setObjectUriAndTitle(UriAndTitle.fromNote(note.getTargetNote()));
    }
    return focusNote;
  }
}
