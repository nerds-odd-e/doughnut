package com.odde.doughnut.services.graphRAG;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.nio.charset.StandardCharsets;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "uri",
  "subjectUriAndTitle",
  "predicate",
  "title",
  "objectUriAndTitle",
  "parentUriAndTitle",
  "relationToFocusNote",
  "details"
})
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

  public static BareNote fromNoteWithoutTruncate(Note note) {
    return new BareNote(note, note.getDetails(), null);
  }

  private static String truncateDetails(String details) {
    if (details == null) {
      return null;
    }

    byte[] bytes = details.getBytes(StandardCharsets.UTF_8);
    if (bytes.length <= RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) {
      return details;
    }

    // Binary search to find the right character position
    int low = 0;
    int high = details.length();

    while (low < high) {
      int mid = (low + high + 1) / 2;
      if (details.substring(0, mid).getBytes(StandardCharsets.UTF_8).length
          <= RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) {
        low = mid;
      } else {
        high = mid - 1;
      }
    }

    return details.substring(0, low) + "...";
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
