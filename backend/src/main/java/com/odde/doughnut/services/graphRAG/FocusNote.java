package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FocusNote extends BareNote {
  private List<String> contextualPath = new ArrayList<>();
  private List<String> children = new ArrayList<>();
  private List<String> referrings = new ArrayList<>();
  private List<String> priorSiblings = new ArrayList<>();
  private List<String> youngerSiblings = new ArrayList<>();

  public static FocusNote fromNote(Note note) {
    FocusNote focusNote = new FocusNote();
    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Self);

    focusNote.setUriAndTitle(bareNote.getUriAndTitle());
    focusNote.setDetails(note.getDetails()); // Don't truncate focus note details
    focusNote.setParentUriAndTitle(bareNote.getParentUriAndTitle());
    focusNote.setObjectUriAndTitle(bareNote.getObjectUriAndTitle());
    focusNote.setRelationToFocusNote(RelationshipToFocusNote.Self);

    // Initialize empty lists for now - we'll add methods to populate these later
    return focusNote;
  }
}
