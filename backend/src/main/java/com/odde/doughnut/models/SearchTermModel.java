package com.odde.doughnut.models;

import com.odde.doughnut.controllers.json.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

  private List<Note> search() {
    if (searchTerm.getAllMyCircles()) {
      return noteRepository.searchForUserInAllMyNotebooksSubscriptionsAndCircle(user, getPattern());
    }
    if (searchTerm.getAllMyNotebooksAndSubscriptions()) {
      return noteRepository.searchForUserInAllMyNotebooksAndSubscriptions(user, getPattern());
    }
    Notebook notebook = null;
    if (searchTerm.note != null) {
      notebook = searchTerm.note.getNotebook();
    }
    return noteRepository.searchInNotebook(notebook, getPattern());
  }

  private String getPattern() {
    return Pattern.quote(searchTerm.getTrimmedSearchKey());
  }

  public List<Note> searchForNotes() {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }

    Integer avoidNoteId = null;
    if (searchTerm.note != null) {
      avoidNoteId = searchTerm.note.getId();
    }
    Integer finalAvoidNoteId = avoidNoteId;
    return search().stream()
        .filter(n -> !n.getId().equals(finalAvoidNoteId))
        .collect(Collectors.toList());
  }
}
