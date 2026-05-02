package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.controllers.dto.FolderTrailSegments;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class FocusNote extends BareNote {
  private final String contextualPath;
  private final List<String> children = new ArrayList<>();
  private final List<String> olderSiblings = new ArrayList<>();
  private final List<String> youngerSiblings = new ArrayList<>();
  private final List<String> links = new ArrayList<>();

  private final List<String> inboundReferences = new ArrayList<>();

  private FocusNote(Note note) {
    super(note, note.getDetails(), RelationshipToFocusNote.Self, null, false, false);
    contextualPath = FolderTrailSegments.crumbPathJoinedBySlashSpace(note);
  }

  public static FocusNote fromNote(Note note) {
    return new FocusNote(note);
  }
}
