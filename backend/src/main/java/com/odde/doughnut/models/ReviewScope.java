package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import java.util.stream.Stream;

public interface ReviewScope {
  Stream<Note> getUnassimilatedNotes();
}
