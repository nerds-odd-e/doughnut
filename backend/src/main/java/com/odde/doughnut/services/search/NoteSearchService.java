package com.odde.doughnut.services.search;

import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.RelationshipLiteralSearchHit;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
  private static final float EXACT_ALIAS_DISTANCE = 0.95f;
  private static final float PARTIAL_ALIAS_DISTANCE = 0.99f;

  private final NoteRepository noteRepository;
  private final NoteAliasSearchService noteAliasSearchService;
  private final RelationshipLiteralHitService relationshipLiteralHitService;
  private final SemanticNoteSearchService semanticNoteSearchService;

  public NoteSearchService(
      NoteRepository noteRepository,
      NoteAliasSearchService noteAliasSearchService,
      RelationshipLiteralHitService relationshipLiteralHitService,
      SemanticNoteSearchService semanticNoteSearchService) {
    this.noteRepository = noteRepository;
    this.noteAliasSearchService = noteAliasSearchService;
    this.relationshipLiteralHitService = relationshipLiteralHitService;
    this.semanticNoteSearchService = semanticNoteSearchService;
  }

  public List<RelationshipLiteralSearchHit> searchForNotes(User user, SearchTerm searchTerm) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }

    List<Note> exactMatches = searchExactMatches(user, searchTerm, null);
    List<Note> partialMatches = searchPartialMatches(user, searchTerm, null);
    List<Note> exactAliasMatches =
        noteAliasSearchService.searchExactAliasMatches(user, searchTerm, null);
    List<Note> partialAliasMatches =
        noteAliasSearchService.searchPartialAliasMatches(user, searchTerm, null);
    List<NoteSearchResult> noteResults =
        combineTitleAndAliasMatches(
            exactMatches, partialMatches, exactAliasMatches, partialAliasMatches, null, null);
    return relationshipLiteralHitService.mergeNoteAndContainerLiteralHits(
        user, searchTerm, null, noteResults);
  }

  public List<RelationshipLiteralSearchHit> searchForNotesInRelationTo(
      User user, SearchTerm searchTerm, Note note) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }
    Integer avoidNoteId = note != null ? note.getId() : null;
    Integer notebookId = note != null ? note.getNotebook().getId() : null;

    List<Note> exactMatches = searchExactMatches(user, searchTerm, notebookId);
    List<Note> partialMatches = searchPartialMatches(user, searchTerm, notebookId);
    List<Note> exactAliasMatches =
        noteAliasSearchService.searchExactAliasMatches(user, searchTerm, notebookId);
    List<Note> partialAliasMatches =
        noteAliasSearchService.searchPartialAliasMatches(user, searchTerm, notebookId);
    List<NoteSearchResult> noteResults =
        combineTitleAndAliasMatches(
            exactMatches,
            partialMatches,
            exactAliasMatches,
            partialAliasMatches,
            avoidNoteId,
            notebookId);
    return relationshipLiteralHitService.mergeNoteAndContainerLiteralHits(
        user, searchTerm, notebookId, noteResults);
  }

  public List<NoteSearchResult> semanticSearchForNotes(User user, SearchTerm searchTerm) {
    return semanticNoteSearchService.semanticSearchForNotes(user, searchTerm);
  }

  public List<NoteSearchResult> semanticSearchForNotesInRelationTo(
      User user, SearchTerm searchTerm, Note note) {
    return semanticNoteSearchService.semanticSearchForNotesInRelationTo(user, searchTerm, note);
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

  private List<NoteSearchResult> combineTitleAndAliasMatches(
      List<Note> exactMatches,
      List<Note> partialMatches,
      List<Note> exactAliasMatches,
      List<Note> partialAliasMatches,
      Integer avoidNoteId,
      Integer notebookId) {
    List<Note> filteredPartialMatches =
        partialMatches.stream()
            .filter(
                note ->
                    exactMatches.stream().noneMatch(exact -> exact.getId().equals(note.getId())))
            .toList();

    List<NoteSearchResult> results =
        exactMatches.stream()
            .filter(note -> !note.getId().equals(avoidNoteId))
            .map(note -> noteToSearchResult(note, 0.0f))
            .collect(Collectors.toList());

    int remainingSlots = exactMatches.isEmpty() ? 20 : 20 + exactMatches.size();

    if (remainingSlots > 0) {
      results.addAll(
          filteredPartialMatches.stream()
              .limit(remainingSlots)
              .filter(note -> !note.getId().equals(avoidNoteId))
              .map(note -> noteToSearchResult(note, 0.9f))
              .collect(Collectors.toList()));
    }

    Set<Integer> titleMatchIds =
        results.stream()
            .map(result -> result.getNoteTopology().getId())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    results.addAll(
        aliasMatchesToSearchResults(
            exactAliasMatches, titleMatchIds, avoidNoteId, EXACT_ALIAS_DISTANCE));
    results.addAll(
        aliasMatchesToSearchResults(
            partialAliasMatches, titleMatchIds, avoidNoteId, PARTIAL_ALIAS_DISTANCE));

    return sortByDistanceThenNotebook(results, notebookId);
  }

  private List<NoteSearchResult> aliasMatchesToSearchResults(
      List<Note> aliasMatches,
      Set<Integer> alreadyMatchedNoteIds,
      Integer avoidNoteId,
      float distance) {
    return aliasMatches.stream()
        .filter(note -> !note.getId().equals(avoidNoteId))
        .filter(note -> alreadyMatchedNoteIds.add(note.getId()))
        .map(note -> noteToSearchResult(note, distance))
        .collect(Collectors.toList());
  }

  private NoteSearchResult noteToSearchResult(Note note, Float distance) {
    return new NoteSearchResult(note, distance);
  }

  private List<NoteSearchResult> sortByDistanceThenNotebook(
      List<NoteSearchResult> results, Integer notebookId) {
    if (notebookId == null) return results;
    return results.stream()
        .sorted(
            (a, b) -> {
              int distCompare =
                  Float.compare(
                      a.getDistance() != null ? a.getDistance() : Float.MAX_VALUE,
                      b.getDistance() != null ? b.getDistance() : Float.MAX_VALUE);
              if (distCompare != 0) return distCompare;
              boolean aSame = notebookId.equals(a.getNotebookId());
              boolean bSame = notebookId.equals(b.getNotebookId());
              return Boolean.compare(bSame, aSame);
            })
        .toList();
  }

  private Pageable getLimitPageable() {
    return PageRequest.of(0, 20);
  }

  private String getPattern(SearchTerm searchTerm) {
    return "%" + searchTerm.getTrimmedSearchKey() + "%";
  }
}
