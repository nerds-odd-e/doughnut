package com.odde.doughnut.services.search;

import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteEmbeddingJdbcRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.EmbeddingService;
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
  private final NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;
  private final EmbeddingService embeddingService;

  public NoteSearchService(
      NoteRepository noteRepository,
      NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository,
      EmbeddingService embeddingService) {
    this.noteRepository = noteRepository;
    this.noteEmbeddingJdbcRepository = noteEmbeddingJdbcRepository;
    this.embeddingService = embeddingService;
  }

  public List<NoteSearchResult> searchForNotes(User user, SearchTerm searchTerm) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }

    List<Note> exactMatches = searchExactMatches(user, searchTerm, null);
    List<Note> partialMatches = searchPartialMatches(user, searchTerm, null);
    return combineExactAndPartialMatches(exactMatches, partialMatches, null);
  }

  public List<NoteSearchResult> searchForNotesInRelationTo(
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

  public List<NoteSearchResult> semanticSearchForNotes(User user, SearchTerm searchTerm) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }
    return semanticSearchInternal(user, searchTerm, null, null);
  }

  public List<NoteSearchResult> semanticSearchForNotesInRelationTo(
      User user, SearchTerm searchTerm, Note note) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }
    Integer avoidNoteId = note != null ? note.getId() : null;
    Integer notebookId = note != null ? note.getNotebook().getId() : null;
    return semanticSearchInternal(user, searchTerm, notebookId, avoidNoteId);
  }

  private List<NoteSearchResult> semanticSearchInternal(
      User user, SearchTerm searchTerm, Integer notebookId, Integer avoidNoteId) {
    List<Float> queryEmbedding =
        embeddingService.generateQueryEmbedding(searchTerm.getTrimmedSearchKey());
    if (queryEmbedding.isEmpty()) {
      return combineExactAndPartialMatches(
          searchExactMatches(user, searchTerm, notebookId),
          searchPartialMatches(user, searchTerm, notebookId),
          avoidNoteId);
    }

    boolean allMyNotebooksAndSubscriptions =
        Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions());
    boolean allMyCircles = Boolean.TRUE.equals(searchTerm.getAllMyCircles());

    var rows =
        noteEmbeddingJdbcRepository.semanticKnnSearch(
            user.getId(),
            notebookId,
            allMyNotebooksAndSubscriptions,
            allMyCircles,
            queryEmbedding,
            20);

    if (rows.isEmpty()) {
      // Fallback to literal search when semantic is unavailable (e.g., local dev)
      return combineExactAndPartialMatches(
          searchExactMatches(user, searchTerm, notebookId),
          searchPartialMatches(user, searchTerm, notebookId),
          avoidNoteId);
    }

    java.util.Map<Integer, Float> noteIdToDistance = new java.util.HashMap<>();
    java.util.List<Integer> orderedIds = new java.util.ArrayList<>();
    for (var r : rows) {
      if (avoidNoteId != null && avoidNoteId.equals(r.noteId)) continue;
      orderedIds.add(r.noteId);
      noteIdToDistance.put(r.noteId, r.combinedDist);
    }
    if (orderedIds.isEmpty()) return List.of();

    // Fetch entities and map preserving order
    List<Note> notes = (List<Note>) noteRepository.findAllById(orderedIds);
    java.util.Map<Integer, Note> idToNote =
        notes.stream().collect(java.util.stream.Collectors.toMap(Note::getId, n -> n));
    return orderedIds.stream()
        .map(id -> idToNote.get(id))
        .filter(java.util.Objects::nonNull)
        .map(n -> new NoteSearchResult(n.getNoteTopology(), noteIdToDistance.get(n.getId())))
        .toList();
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

  private List<NoteSearchResult> combineExactAndPartialMatches(
      List<Note> exactMatches, List<Note> partialMatches, Integer avoidNoteId) {
    List<Note> filteredPartialMatches =
        partialMatches.stream()
            .filter(
                note ->
                    exactMatches.stream().noneMatch(exact -> exact.getId().equals(note.getId())))
            .toList();

    List<NoteSearchResult> results =
        exactMatches.stream()
            .filter(note -> !note.getId().equals(avoidNoteId))
            .map(note -> new NoteSearchResult(note.getNoteTopology(), /* distance= */ 0.0f))
            .collect(Collectors.toList());

    int remainingSlots = exactMatches.isEmpty() ? 20 : 20 + exactMatches.size();

    if (remainingSlots > 0) {
      results.addAll(
          filteredPartialMatches.stream()
              .limit(remainingSlots)
              .filter(note -> !note.getId().equals(avoidNoteId))
              .map(note -> new NoteSearchResult(note.getNoteTopology(), /* distance= */ 0.9f))
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
