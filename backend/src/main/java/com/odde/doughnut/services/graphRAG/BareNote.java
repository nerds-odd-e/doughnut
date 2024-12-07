package com.odde.doughnut.services.graphRAG;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.entities.Note;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BareNote {
  private final Note note;
  @Getter private final String details;
  @Getter private final RelationshipToFocusNote relationToFocusNote;

  protected BareNote(Note note, String details, RelationshipToFocusNote relation) {
    this.note = note;
    this.details = details;
    this.relationToFocusNote = relation;
  }

  @JsonIgnore
  public UriAndTitle getUriAndTitle() {
    return UriAndTitle.fromNote(note);
  }

  @JsonProperty("uri")
  public String getUri() {
    return note.getUri();
  }

  @JsonProperty("title")
  public String getTitle() {
    return getObjectUriAndTitle() != null ? null : note.getTopicConstructor();
  }

  @JsonProperty("predicate")
  public String getPredicate() {
    return getObjectUriAndTitle() != null ? note.getTopicConstructor() : null;
  }

  @JsonProperty("objectUriAndTitle")
  public UriAndTitle getObjectUriAndTitle() {
    return note.getTargetNote() != null ? UriAndTitle.fromNote(note.getTargetNote()) : null;
  }

  @JsonProperty("parentUriAndTitle")
  public UriAndTitle getParentUriAndTitle() {
    return getObjectUriAndTitle() == null && note.getParent() != null
        ? UriAndTitle.fromNote(note.getParent())
        : null;
  }

  @JsonProperty("subjectUriAndTitle")
  public UriAndTitle getSubjectUriAndTitle() {
    return getObjectUriAndTitle() != null && note.getParent() != null
        ? UriAndTitle.fromNote(note.getParent())
        : null;
  }

  public static BareNote fromNote(Note note, RelationshipToFocusNote relation) {
    return new BareNote(note, truncateDetails(note.getDetails()), relation);
  }

  private static String truncateDetails(String details) {
    if (details == null || details.length() <= RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) {
      return details;
    }
    return details.substring(0, RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "...";
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Note
        ? getUriAndTitle().equals(obj)
        : obj instanceof BareNote && getUriAndTitle().equals(((BareNote) obj).getUriAndTitle());
  }

  @Override
  public int hashCode() {
    return getUriAndTitle().hashCode();
  }
}
