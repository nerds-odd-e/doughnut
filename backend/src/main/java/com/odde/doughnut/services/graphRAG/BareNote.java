package com.odde.doughnut.services.graphRAG;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.entities.Note;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BareNote {
  @Getter private UriAndTitle uriAndTitle;
  @Getter private String details;
  private UriAndTitle parentUriAndTitle;
  @Getter private UriAndTitle objectUriAndTitle;
  @Getter private RelationshipToFocusNote relationToFocusNote;

  @JsonProperty("parentUriAndTitle")
  public UriAndTitle getParentUriAndTitle() {
    return objectUriAndTitle == null ? parentUriAndTitle : null;
  }

  @JsonProperty("subjectUriAndTitle")
  public UriAndTitle getSubjectUriAndTitle() {
    return objectUriAndTitle != null ? parentUriAndTitle : null;
  }

  protected static void initializeFromNote(
      BareNote bareNote, Note note, RelationshipToFocusNote relation) {
    bareNote.uriAndTitle = UriAndTitle.fromNote(note);
    bareNote.details = note.getDetails();
    bareNote.relationToFocusNote = relation;

    if (note.getParent() != null) {
      bareNote.parentUriAndTitle = UriAndTitle.fromNote(note.getParent());
    }
    if (note.getTargetNote() != null) {
      bareNote.objectUriAndTitle = UriAndTitle.fromNote(note.getTargetNote());
    }
  }

  public static BareNote fromNote(Note note, RelationshipToFocusNote relation) {
    BareNote bareNote = new BareNote();
    initializeFromNote(bareNote, note, relation);
    bareNote.details = truncateDetails(bareNote.details);
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
