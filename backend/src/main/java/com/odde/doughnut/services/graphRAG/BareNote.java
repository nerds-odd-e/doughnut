package com.odde.doughnut.services.graphRAG;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.entities.Note;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BareNote {
  private UriAndTitle uriAndTitle;
  private String details;
  private UriAndTitle parentUriAndTitle;
  private UriAndTitle objectUriAndTitle;
  private RelationshipToFocusNote relationToFocusNote;

  @JsonProperty("parentUriAndTitle")
  public UriAndTitle getParentUriAndTitleJson() {
    return objectUriAndTitle == null ? parentUriAndTitle : null;
  }

  @JsonProperty("subjectUriAndTitle")
  public UriAndTitle getSubjectUriAndTitleJson() {
    return objectUriAndTitle != null ? parentUriAndTitle : null;
  }

  protected static void initializeFromNote(
      BareNote bareNote, Note note, RelationshipToFocusNote relation) {
    bareNote.setUriAndTitle(UriAndTitle.fromNote(note));
    bareNote.setDetails(note.getDetails());
    bareNote.setRelationToFocusNote(relation);

    if (note.getParent() != null) {
      bareNote.setParentUriAndTitle(UriAndTitle.fromNote(note.getParent()));
    }
    if (note.getTargetNote() != null) {
      bareNote.setObjectUriAndTitle(UriAndTitle.fromNote(note.getTargetNote()));
    }
  }

  public static BareNote fromNote(Note note, RelationshipToFocusNote relation) {
    BareNote bareNote = new BareNote();
    initializeFromNote(bareNote, note, relation);
    bareNote.setDetails(truncateDetails(bareNote.getDetails()));
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
