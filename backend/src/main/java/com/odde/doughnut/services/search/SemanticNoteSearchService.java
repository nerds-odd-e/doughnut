package com.odde.doughnut.services.search;

import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteEmbeddingJdbcRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.EmbeddingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SemanticNoteSearchService {
  private final NoteRepository noteRepository;
  private final NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;
  private final EmbeddingService embeddingService;

  public SemanticNoteSearchService(
      NoteRepository noteRepository,
      NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository,
      EmbeddingService embeddingService) {
    this.noteRepository = noteRepository;
    this.noteEmbeddingJdbcRepository = noteEmbeddingJdbcRepository;
    this.embeddingService = embeddingService;
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
      return List.of();
    }

    var rows =
        noteEmbeddingJdbcRepository.semanticKnnSearch(
            user.getId(),
            notebookId,
            Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions()),
            Boolean.TRUE.equals(searchTerm.getAllMyCircles()),
            queryEmbedding,
            20);

    var noteIdToDistance = new HashMap<Integer, Float>();
    var orderedIds = new ArrayList<Integer>();
    for (var row : rows) {
      if (avoidNoteId != null && avoidNoteId.equals(row.noteId)) {
        continue;
      }
      orderedIds.add(row.noteId);
      noteIdToDistance.put(row.noteId, row.combinedDist);
    }
    if (orderedIds.isEmpty()) {
      return List.of();
    }

    List<Note> notes = (List<Note>) noteRepository.findAllById(orderedIds);
    var idToNote = notes.stream().collect(Collectors.toMap(Note::getId, note -> note));
    List<NoteSearchResult> results =
        orderedIds.stream()
            .map(idToNote::get)
            .filter(Objects::nonNull)
            .map(note -> new NoteSearchResult(note, noteIdToDistance.get(note.getId())))
            .toList();

    return sortByDistanceThenNotebook(results, notebookId);
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
}
