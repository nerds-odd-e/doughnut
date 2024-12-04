package com.odde.doughnut.models;

import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;

public class SearchTermModel {
  private final User user;
  private final SearchTerm searchTerm;
  NoteRepository noteRepository;

  public SearchTermModel(User entity, NoteRepository noteRepository, SearchTerm searchTerm) {
    this.user = entity;
    this.searchTerm = searchTerm;
    this.noteRepository = noteRepository;
  }

  private Stream<Note> search(Integer notebookId) {
    if (searchTerm.getAllMyCircles()) {
      return Stream.concat(
          searchInMyNotebooksAndSubscriptions(),
          noteRepository.searchForUserInAllMyCircle(user.getId(), getPattern()));
    }
    if (searchTerm.getAllMyNotebooksAndSubscriptions()) {
      return searchInMyNotebooksAndSubscriptions();
    }
    return noteRepository.searchInNotebook(notebookId, getPattern());
  }

  private Stream<Note> searchInMyNotebooksAndSubscriptions() {
    return Stream.concat(
        searchInMyNotebooks(),
        noteRepository.searchForUserInAllMySubscriptions(user.getId(), getPattern()));
  }

  private Stream<Note> searchInMyNotebooks() {
    return noteRepository.searchForUserInAllMyNotebooks(user.getId(), getPattern());
  }

  private String getPattern() {
    return "%" + searchTerm.getTrimmedSearchKey() + "%";
  }

  public List<NoteTopology> searchForNotesInRelateTo(Note note) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }
    Integer avoidNoteId = null;
    if (note != null) {
      avoidNoteId = note.getId();
    }
    Integer finalAvoidNoteId = avoidNoteId;
    return search(note.getNotebook().getId())
        .filter(n -> !n.getId().equals(finalAvoidNoteId))
        .map(Note::getNoteTopology)
        .toList();
  }

  public List<NoteTopology> searchForNotes() {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }
    return search(null).map(Note::getNoteTopology).toList();
  }
}
