package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import java.util.Map;
import java.util.Optional;

/** The Git states of the notebooks an accepted web change locked, keyed by notebook id. */
public record LockedNotebooks(Map<Integer, NotebookGitStateLoader.LockedNotebookState> states) {

  public Optional<NotebookGitStateLoader.LockedNotebookState> state(Integer notebookId) {
    return Optional.ofNullable(states.get(notebookId));
  }

  public Optional<Note> storedNote(Integer noteId) {
    return states.values().stream()
        .flatMap(state -> state.storedNotes().stream())
        .filter(note -> noteId.equals(note.getId()))
        .findFirst();
  }
}
