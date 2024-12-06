package com.odde.doughnut.services.graphRAG;

import com.fasterxml.jackson.annotation.JsonValue;
import com.odde.doughnut.entities.Note;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class UriAndTitle {
  private final String markdownLink;

  public UriAndTitle(String title, String uri) {
    this.markdownLink = String.format("[%s](%s)", title, uri);
  }

  @JsonValue
  @Override
  public String toString() {
    return markdownLink;
  }

  public static UriAndTitle fromNote(Note note) {
    return new UriAndTitle(note.getTopicConstructor(), "/n" + note.getId());
  }
}
