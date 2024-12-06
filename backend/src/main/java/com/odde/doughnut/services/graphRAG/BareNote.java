package com.odde.doughnut.services.graphRAG;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;

import com.odde.doughnut.entities.Note;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BareNote {
  private String uriAndTitle;
  private String details;
  private String parentUriAndTitle;
  private String objectUriAndTitle;
  private RelationshipToFocusNote relationToFocusNote;

  public static BareNote fromNote(Note note, RelationshipToFocusNote relation) {
    BareNote bareNote = new BareNote();
    bareNote.setUriAndTitle(note.getUriAndTitle());
    bareNote.setDetails(truncateDetails(note.getDetails()));
    if (note.getParent() != null) {
      bareNote.setParentUriAndTitle(note.getParent().getUriAndTitle());
    }
    if (note.getTargetNote() != null) {
      bareNote.setObjectUriAndTitle(note.getTargetNote().getUriAndTitle());
    }
    bareNote.setRelationToFocusNote(relation);
    return bareNote;
  }

  private static String truncateDetails(String details) {
    if (details == null || details.length() <= RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) {
      return details;
    }
    return details.substring(0, RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "...";
  }
}
