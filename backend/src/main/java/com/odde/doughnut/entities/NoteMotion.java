package com.odde.doughnut.entities;

import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class NoteMotion {
  private final Note subject;
  private final Note relativeToNote;
  private final boolean asFirstChildOfNote;

  public Note getNewParent() {
    if (asFirstChildOfNote) {
      return relativeToNote;
    }
    return relativeToNote.getParent();
  }

  public void moveHeadNoteOnly() throws CyclicLinkDetectedException {
    if (relativeToNote.getAncestors().contains(subject)) {
      throw new CyclicLinkDetectedException();
    }
    subject.updateSiblingOrder(relativeToNote, asFirstChildOfNote);
  }
}
