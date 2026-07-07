package com.odde.doughnut.services.search;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAliasIndex;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteAliasIndexRepository;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoteAliasSearchService {
  private final NoteAliasIndexRepository noteAliasIndexRepository;

  public NoteAliasSearchService(NoteAliasIndexRepository noteAliasIndexRepository) {
    this.noteAliasIndexRepository = noteAliasIndexRepository;
  }

  public List<Note> searchExactAliasMatches(User user, SearchTerm searchTerm, Integer notebookId) {
    String exactSearchKey = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(exactSearchKey)) {
      return List.of();
    }

    String aliasLookupKey = FrontmatterAliases.normalizedLookupKey(exactSearchKey);
    if (Boolean.TRUE.equals(searchTerm.getAllMyCircles())) {
      return Stream.concat(
              searchExactAliasMatchesInMyNotebooksAndSubscriptions(user, aliasLookupKey).stream(),
              aliasRowsToNotes(
                  noteAliasIndexRepository.searchExactForUserInAllMyCircle(
                      user.getId(), aliasLookupKey))
                  .stream())
          .toList();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions())) {
      return searchExactAliasMatchesInMyNotebooksAndSubscriptions(user, aliasLookupKey);
    }
    return aliasRowsToNotes(
        noteAliasIndexRepository.searchExactInNotebook(notebookId, aliasLookupKey));
  }

  public List<Note> searchPartialAliasMatches(
      User user, SearchTerm searchTerm, Integer notebookId) {
    String searchKey = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(searchKey)) {
      return List.of();
    }

    if (Boolean.TRUE.equals(searchTerm.getAllMyCircles())) {
      return Stream.concat(
              searchPartialAliasMatchesInMyNotebooksAndSubscriptions(user, searchTerm).stream(),
              aliasRowsToNotes(
                  noteAliasIndexRepository.searchForUserInAllMyCircle(
                      user.getId(), getAliasPattern(searchTerm), getLimitPageable()))
                  .stream())
          .toList();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions())) {
      return searchPartialAliasMatchesInMyNotebooksAndSubscriptions(user, searchTerm);
    }
    return aliasRowsToNotes(
        noteAliasIndexRepository.searchInNotebook(
            notebookId, getAliasPattern(searchTerm), getLimitPageable()));
  }

  private List<Note> searchExactAliasMatchesInMyNotebooksAndSubscriptions(
      User user, String aliasLookupKey) {
    return Stream.concat(
            aliasRowsToNotes(
                noteAliasIndexRepository.searchExactForUserInAllMyNotebooks(
                    user.getId(), aliasLookupKey))
                .stream(),
            aliasRowsToNotes(
                noteAliasIndexRepository.searchExactForUserInAllMySubscriptions(
                    user.getId(), aliasLookupKey))
                .stream())
        .toList();
  }

  private List<Note> searchPartialAliasMatchesInMyNotebooksAndSubscriptions(
      User user, SearchTerm searchTerm) {
    return Stream.concat(
            aliasRowsToNotes(
                noteAliasIndexRepository.searchForUserInAllMyNotebooks(
                    user.getId(), getAliasPattern(searchTerm), getLimitPageable()))
                .stream(),
            aliasRowsToNotes(
                noteAliasIndexRepository.searchForUserInAllMySubscriptions(
                    user.getId(), getAliasPattern(searchTerm), getLimitPageable()))
                .stream())
        .toList();
  }

  private List<Note> aliasRowsToNotes(List<NoteAliasIndex> rows) {
    return rows.stream().map(NoteAliasIndex::getNote).toList();
  }

  private Pageable getLimitPageable() {
    return PageRequest.of(0, 20);
  }

  private String getAliasPattern(SearchTerm searchTerm) {
    return "%" + FrontmatterAliases.normalizedLookupKey(searchTerm.getTrimmedSearchKey()) + "%";
  }
}
