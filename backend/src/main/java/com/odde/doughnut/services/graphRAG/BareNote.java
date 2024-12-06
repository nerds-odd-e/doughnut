package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;

public class BareNote {
  private String uriAndTitle;
  private String details;
  private String parentUriAndTitle;
  private String objectUriAndTitle;
  private RelationshipToFocusNote relationToFocusNote;

  public static BareNote fromNote(Note note, RelationshipToFocusNote relation) {
    BareNote bareNote = new BareNote();
    bareNote.setUriAndTitle(formatUriAndTitle(note));
    bareNote.setDetails(truncateDetails(note.getDetails()));
    if (note.getParent() != null) {
      bareNote.setParentUriAndTitle(formatUriAndTitle(note.getParent()));
    }
    if (note.getTargetNote() != null) {
      bareNote.setObjectUriAndTitle(formatUriAndTitle(note.getTargetNote()));
    }
    bareNote.setRelationToFocusNote(relation);
    return bareNote;
  }

  private static String formatUriAndTitle(Note note) {
    return String.format("[%s](%s)", note.getTopicConstructor(), note.getUri());
  }

  private static String truncateDetails(String details) {
    if (details == null
        || details.length() <= GraphRAGService.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) {
      return details;
    }
    return details.substring(0, GraphRAGService.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "...";
  }

  // Getters and setters
  public String getUriAndTitle() {
    return uriAndTitle;
  }

  public void setUriAndTitle(String uriAndTitle) {
    this.uriAndTitle = uriAndTitle;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public String getParentUriAndTitle() {
    return parentUriAndTitle;
  }

  public void setParentUriAndTitle(String parentUriAndTitle) {
    this.parentUriAndTitle = parentUriAndTitle;
  }

  public String getObjectUriAndTitle() {
    return objectUriAndTitle;
  }

  public void setObjectUriAndTitle(String objectUriAndTitle) {
    this.objectUriAndTitle = objectUriAndTitle;
  }

  public RelationshipToFocusNote getRelationToFocusNote() {
    return relationToFocusNote;
  }

  public void setRelationToFocusNote(RelationshipToFocusNote relationToFocusNote) {
    this.relationToFocusNote = relationToFocusNote;
  }
}
