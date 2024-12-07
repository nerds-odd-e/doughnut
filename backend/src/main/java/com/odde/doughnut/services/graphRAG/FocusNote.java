package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class FocusNote extends BareNote {
  private final List<UriAndTitle> children = new ArrayList<>();
  private final List<UriAndTitle> priorSiblings = new ArrayList<>();
  private final List<UriAndTitle> youngerSiblings = new ArrayList<>();
  private final List<UriAndTitle> contextualPath = new ArrayList<>();
  private final List<UriAndTitle> inboundReferences = new ArrayList<>();

  private FocusNote(Note note) {
    super(note, note.getDetails(), RelationshipToFocusNote.Self);

    // Add contextual path (unique to FocusNote)
    if (note.getParent() != null) {
      note.getAncestors().forEach(ancestor -> contextualPath.add(UriAndTitle.fromNote(ancestor)));
    }
  }

  public static FocusNote fromNote(Note note) {
    return new FocusNote(note);
  }
}
