package com.odde.doughnut.models;

import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class SearchTermModel {
  private final User user;
  private final SearchTerm searchTerm;
  NoteRepository noteRepository;

  public SearchTermModel(User entity, NoteRepository noteRepository, SearchTerm searchTerm) {
    this.user = entity;
    this.noteRepository = noteRepository;
    this.searchTerm = searchTerm;
  }

  public List<NoteTopology> search(Integer notebookId, Integer avoidNoteId) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }

    // First, search for exact matches using dedicated repository methods
    List<Note> exactMatches = searchExactMatches(notebookId);
    
    // Then, search for partial matches with normal limit
    List<Note> partialMatches = searchPartialMatches(notebookId);
    
    // Combine and prioritize exact matches first
    return combineExactAndPartialMatches(exactMatches, partialMatches, avoidNoteId);
  }

  private List<Note> searchExactMatches(Integer notebookId) {
    String exactSearchKey = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(exactSearchKey)) {
      return List.of();
    }

    // Use dedicated repository methods for exact matching
    if (searchTerm.getAllMyCircles()) {
      return Stream.concat(
          searchExactMatchesInMyNotebooksAndSubscriptions().stream(),
          noteRepository.searchExactForUserInAllMyCircle(user.getId(), exactSearchKey).stream())
          .toList();
    }
    if (searchTerm.getAllMyNotebooksAndSubscriptions()) {
      return searchExactMatchesInMyNotebooksAndSubscriptions();
    }
    return noteRepository.searchExactInNotebook(notebookId, exactSearchKey);
  }

  private List<Note> searchExactMatchesInMyNotebooksAndSubscriptions() {
    return Stream.concat(
        noteRepository.searchExactForUserInAllMyNotebooks(user.getId(), searchTerm.getTrimmedSearchKey()).stream(),
        noteRepository.searchExactForUserInAllMySubscriptions(user.getId(), searchTerm.getTrimmedSearchKey()).stream())
        .toList();
  }

  private List<Note> searchPartialMatches(Integer notebookId) {
    String searchKey = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(searchKey)) {
      return List.of();
    }

    // Use existing LIKE search methods for partial matches
    if (searchTerm.getAllMyCircles()) {
      return Stream.concat(
          searchPartialMatchesInMyNotebooksAndSubscriptions().stream(),
          noteRepository.searchForUserInAllMyCircle(user.getId(), getPattern(), getLimitPageable()).stream())
          .toList();
    }
    if (searchTerm.getAllMyNotebooksAndSubscriptions()) {
      return searchPartialMatchesInMyNotebooksAndSubscriptions();
    }
    return noteRepository.searchInNotebook(notebookId, getPattern(), getLimitPageable());
  }

  private List<Note> searchPartialMatchesInMyNotebooksAndSubscriptions() {
    return Stream.concat(
        noteRepository.searchForUserInAllMyNotebooks(user.getId(), getPattern(), getLimitPageable()).stream(),
        noteRepository.searchForUserInAllMySubscriptions(user.getId(), getPattern(), getLimitPageable()).stream())
        .toList();
  }

  private List<NoteTopology> combineExactAndPartialMatches(List<Note> exactMatches, List<Note> partialMatches, Integer avoidNoteId) {
    // Filter out exact matches from partial matches to avoid duplicates
    List<Note> filteredPartialMatches = partialMatches.stream()
        .filter(note -> exactMatches.stream().noneMatch(exact -> exact.getId().equals(note.getId())))
        .toList();

    // Combine exact matches first, then partial matches
    List<NoteTopology> results = exactMatches.stream()
        .filter(note -> !note.getId().equals(avoidNoteId))
        .map(Note::getNoteTopology)
        .collect(Collectors.toList());

    // If we have exact matches, we can exceed the normal limit to include more partial matches
    // This ensures exact matches are always included even when there are many partial matches
    int remainingSlots = exactMatches.isEmpty() ? 20 : 20 + exactMatches.size();
    
    if (remainingSlots > 0) {
      results.addAll(filteredPartialMatches.stream()
          .limit(remainingSlots)
          .filter(note -> !note.getId().equals(avoidNoteId))
          .map(Note::getNoteTopology)
          .collect(Collectors.toList()));
    }

    return results;
  }

  private Pageable getLimitPageable() {
    return PageRequest.of(0, 20);
  }

  private String getPattern() {
    return "%" + searchTerm.getTrimmedSearchKey() + "%";
  }

  public List<NoteTopology> searchForNotes() {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }
    
    // Search for exact matches first
    List<Note> exactMatches = searchExactMatches(null);
    
    // Search for partial matches
    List<Note> partialMatches = searchPartialMatches(null);
    
    return combineExactAndPartialMatches(exactMatches, partialMatches, null);
  }

  public List<NoteTopology> searchForNotesInRelateTo(Note note) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }
    Integer avoidNoteId = note != null ? note.getId() : null;
    
    // Search for exact matches first
    List<Note> exactMatches = searchExactMatches(note != null ? note.getNotebook().getId() : null);
    
    // Search for partial matches
    List<Note> partialMatches = searchPartialMatches(note != null ? note.getNotebook().getId() : null);
    
    return combineExactAndPartialMatches(exactMatches, partialMatches, avoidNoteId);
  }
}
