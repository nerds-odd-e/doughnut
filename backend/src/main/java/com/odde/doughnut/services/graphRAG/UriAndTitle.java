package com.odde.doughnut.services.graphRAG;

import com.fasterxml.jackson.annotation.JsonValue;
import com.odde.doughnut.entities.Note;
import lombok.Getter;

public class UriAndTitle {
  @Getter private final Note note;

  private UriAndTitle(Note note) {
    this.note = note;
  }

  @JsonValue
  @Override
  public String toString() {
    return String.format("[%s](%s)", note.getTopicConstructor(), "/n" + note.getId());
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
