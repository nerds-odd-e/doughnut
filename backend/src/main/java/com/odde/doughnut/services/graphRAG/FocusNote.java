package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

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

  // Getters and setters
  public List<String> getContextualPath() {
    return contextualPath;
  }

  public void setContextualPath(List<String> contextualPath) {
    this.contextualPath = contextualPath;
  }

  public List<String> getChildren() {
    return children;
  }

  public void setChildren(List<String> children) {
    this.children = children;
  }

  public List<String> getReferrings() {
    return referrings;
  }

  public void setReferrings(List<String> referrings) {
    this.referrings = referrings;
  }

  public List<String> getPriorSiblings() {
    return priorSiblings;
  }

  public void setPriorSiblings(List<String> priorSiblings) {
    this.priorSiblings = priorSiblings;
  }

  public List<String> getYoungerSiblings() {
    return youngerSiblings;
  }

  public void setYoungerSiblings(List<String> youngerSiblings) {
    this.youngerSiblings = youngerSiblings;
  }
}
