package com.odde.doughnut.services.graphRAG;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Note;
import lombok.Getter;

@JsonPropertyOrder({"uri", "title"})
public class UriAndTitle {
  @Getter @JsonIgnore private final Note note;

  private UriAndTitle(Note note) {
    this.note = note;
  }

  @JsonProperty("uri")
  public String getUri() {
    return note.getUri();
  }

  @JsonProperty("title")
  public String getTitle() {
    return note.getTitleConstructor();
  }

  public static UriAndTitle fromNote(Note note) {
    return new UriAndTitle(note);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof UriAndTitle) {
      return note.equals(((UriAndTitle) obj).note);
    }
    if (obj instanceof Note) {
      return note.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return note.hashCode();
  }
}
