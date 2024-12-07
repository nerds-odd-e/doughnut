package com.odde.doughnut.services.graphRAG;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;

import com.odde.doughnut.entities.Note;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BareNote {
  private UriAndTitle uriAndTitle;
  private String details;
  private UriAndTitle parentUriAndTitle;
  private UriAndTitle objectUriAndTitle;
  private RelationshipToFocusNote relationToFocusNote;

  public static BareNote fromNote(Note note, RelationshipToFocusNote relation) {
    BareNote bareNote = new BareNote();
    bareNote.setUriAndTitle(UriAndTitle.fromNote(note));
    bareNote.setDetails(truncateDetails(note.getDetails()));
    if (note.getParent() != null) {
      bareNote.setParentUriAndTitle(UriAndTitle.fromNote(note.getParent()));
    }
    if (note.getTargetNote() != null) {
      bareNote.setObjectUriAndTitle(UriAndTitle.fromNote(note.getTargetNote()));
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

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BareNote) {
      return uriAndTitle.equals(((BareNote) obj).uriAndTitle);
    }
    if (obj instanceof Note) {
      return uriAndTitle.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return uriAndTitle.hashCode();
  }
}
