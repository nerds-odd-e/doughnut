package com.odde.doughnut.entities;

import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import lombok.Getter;
import lombok.Setter;

public class NoteMotion {
  @Getter @Setter Note subject;
  @Getter @Setter Note relativeToNote;
  @Getter @Setter private boolean asFirstChildOfNote;

  public NoteMotion(Note relativeToNote, boolean asFirstChildOfNote) {
    this.relativeToNote = relativeToNote;
    this.asFirstChildOfNote = asFirstChildOfNote;
  }

  public Note getNewParent() {
    if (asFirstChildOfNote) {
      return relativeToNote;
    }
    return relativeToNote.getParentNote();
  }

  public void moveHeadNoteOnly() throws CyclicLinkDetectedException {
    if (relativeToNote.getAncestors().contains(subject)) {
      throw new CyclicLinkDetectedException();
    }
    subject.updateSiblingOrder(relativeToNote, asFirstChildOfNote);
  }
}
