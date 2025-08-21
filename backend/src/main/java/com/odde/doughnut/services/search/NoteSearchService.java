package com.odde.doughnut.services.search;

import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoteSearchService {
  private final NoteRepository noteRepository;

  public NoteSearchService(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  public List<NoteTopology> searchForNotes(User user, SearchTerm searchTerm) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }

    List<Note> exactMatches = searchExactMatches(user, searchTerm, null);
    List<Note> partialMatches = searchPartialMatches(user, searchTerm, null);
    return combineExactAndPartialMatches(exactMatches, partialMatches, null);
  }

  public List<NoteTopology> searchForNotesInRelationTo(
      User user, SearchTerm searchTerm, Note note) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }
    Integer avoidNoteId = note != null ? note.getId() : null;
    Integer notebookId = note != null ? note.getNotebook().getId() : null;

    List<Note> exactMatches = searchExactMatches(user, searchTerm, notebookId);
    List<Note> partialMatches = searchPartialMatches(user, searchTerm, notebookId);
    return combineExactAndPartialMatches(exactMatches, partialMatches, avoidNoteId);
  }

  private List<Note> searchExactMatches(User user, SearchTerm searchTerm, Integer notebookId) {
    String exactSearchKey = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(exactSearchKey)) {
      return List.of();
    }

    if (Boolean.TRUE.equals(searchTerm.getAllMyCircles())) {
      return Stream.concat(
              searchExactMatchesInMyNotebooksAndSubscriptions(user, searchTerm).stream(),
              noteRepository.searchExactForUserInAllMyCircle(user.getId(), exactSearchKey).stream())
          .toList();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions())) {
      return searchExactMatchesInMyNotebooksAndSubscriptions(user, searchTerm);
    }
    return noteRepository.searchExactInNotebook(notebookId, exactSearchKey);
  }

  private List<Note> searchExactMatchesInMyNotebooksAndSubscriptions(
      User user, SearchTerm searchTerm) {
    return Stream.concat(
            noteRepository
                .searchExactForUserInAllMyNotebooks(user.getId(), searchTerm.getTrimmedSearchKey())
                .stream(),
            noteRepository
                .searchExactForUserInAllMySubscriptions(
                    user.getId(), searchTerm.getTrimmedSearchKey())
                .stream())
        .toList();
  }

  private List<Note> searchPartialMatches(User user, SearchTerm searchTerm, Integer notebookId) {
    String searchKey = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(searchKey)) {
      return List.of();
    }

    if (Boolean.TRUE.equals(searchTerm.getAllMyCircles())) {
      return Stream.concat(
              searchPartialMatchesInMyNotebooksAndSubscriptions(user, searchTerm).stream(),
              noteRepository
                  .searchForUserInAllMyCircle(
                      user.getId(), getPattern(searchTerm), getLimitPageable())
                  .stream())
          .toList();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions())) {
      return searchPartialMatchesInMyNotebooksAndSubscriptions(user, searchTerm);
    }
    return noteRepository.searchInNotebook(notebookId, getPattern(searchTerm), getLimitPageable());
  }

  private List<Note> searchPartialMatchesInMyNotebooksAndSubscriptions(
      User user, SearchTerm searchTerm) {
    return Stream.concat(
            noteRepository
                .searchForUserInAllMyNotebooks(
                    user.getId(), getPattern(searchTerm), getLimitPageable())
                .stream(),
            noteRepository
                .searchForUserInAllMySubscriptions(
                    user.getId(), getPattern(searchTerm), getLimitPageable())
                .stream())
        .toList();
  }

  private List<NoteTopology> combineExactAndPartialMatches(
      List<Note> exactMatches, List<Note> partialMatches, Integer avoidNoteId) {
    List<Note> filteredPartialMatches =
        partialMatches.stream()
            .filter(
                note ->
                    exactMatches.stream().noneMatch(exact -> exact.getId().equals(note.getId())))
            .toList();

    List<NoteTopology> results =
        exactMatches.stream()
            .filter(note -> !note.getId().equals(avoidNoteId))
            .map(Note::getNoteTopology)
            .collect(Collectors.toList());

    int remainingSlots = exactMatches.isEmpty() ? 20 : 20 + exactMatches.size();

    if (remainingSlots > 0) {
      results.addAll(
          filteredPartialMatches.stream()
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

  private String getPattern(SearchTerm searchTerm) {
    return "%" + searchTerm.getTrimmedSearchKey() + "%";
  }
}
