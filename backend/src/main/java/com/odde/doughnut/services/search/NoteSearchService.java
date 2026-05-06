package com.odde.doughnut.services.search;

import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.RelationshipLiteralSearchHit;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteEmbeddingJdbcRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.services.EmbeddingService;
import java.util.ArrayList;
import java.util.Comparator;
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
  private static final int FOLDER_LITERAL_MATCH_CAP = 12;
  private static final int NOTEBOOK_LITERAL_MATCH_CAP = 12;

  private final NoteRepository noteRepository;
  private final FolderRepository folderRepository;
  private final NotebookRepository notebookRepository;
  private final NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;
  private final EmbeddingService embeddingService;

  public NoteSearchService(
      NoteRepository noteRepository,
      FolderRepository folderRepository,
      NotebookRepository notebookRepository,
      NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository,
      EmbeddingService embeddingService) {
    this.noteRepository = noteRepository;
    this.folderRepository = folderRepository;
    this.notebookRepository = notebookRepository;
    this.noteEmbeddingJdbcRepository = noteEmbeddingJdbcRepository;
    this.embeddingService = embeddingService;
  }

  public List<RelationshipLiteralSearchHit> searchForNotes(User user, SearchTerm searchTerm) {
    if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
      return List.of();
    }

    List<Note> exactMatches = searchExactMatches(user, searchTerm, null);
    List<Note> partialMatches = searchPartialMatches(user, searchTerm, null);
    List<NoteSearchResult> noteResults =
        combineExactAndPartialMatches(exactMatches, partialMatches, null, null);
    return mergeNoteAndFolderLiteralHits(user, searchTerm, null, noteResults);
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
    List<NoteSearchResult> noteResults =
        combineExactAndPartialMatches(exactMatches, partialMatches, avoidNoteId, notebookId);
    return mergeNoteAndFolderLiteralHits(user, searchTerm, notebookId, noteResults);
  }

  private List<RelationshipLiteralSearchHit> mergeNoteAndFolderLiteralHits(
      User user, SearchTerm searchTerm, Integer notebookId, List<NoteSearchResult> noteResults) {
    List<RelationshipLiteralSearchHit> hits = new ArrayList<>();
    for (NoteSearchResult r : noteResults) {
      hits.add(RelationshipLiteralSearchHit.note(r));
    }
    List<Folder> exactFolders = searchExactFolderMatches(user, searchTerm, notebookId);
    List<Folder> partialFolders = searchPartialFolderMatches(user, searchTerm, notebookId);
    hits.addAll(combineFolderExactAndPartialAsHits(exactFolders, partialFolders));
    List<Notebook> exactNotebooks = searchExactNotebookMatches(user, searchTerm, notebookId);
    List<Notebook> partialNotebooks = searchPartialNotebookMatches(user, searchTerm, notebookId);
    hits.addAll(combineNotebookExactAndPartialAsHits(exactNotebooks, partialNotebooks));
    return sortRelationshipLiteralHits(hits, notebookId);
  }

  private boolean suppressNotebookLiteralHits(SearchTerm searchTerm, Integer notebookId) {
    if (notebookId == null) {
      return false;
    }
    return !Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions())
        && !Boolean.TRUE.equals(searchTerm.getAllMyCircles());
  }

  private List<RelationshipLiteralSearchHit> combineNotebookExactAndPartialAsHits(
      List<Notebook> exactNotebooks, List<Notebook> partialNotebooks) {
    List<RelationshipLiteralSearchHit> notebookHits = new ArrayList<>();
    for (Notebook nb : exactNotebooks) {
      notebookHits.add(notebookToHit(nb, 0.0f));
    }
    java.util.Set<Integer> exactNotebookIds =
        exactNotebooks.stream().map(Notebook::getId).collect(Collectors.toSet());
    int remaining = Math.max(0, NOTEBOOK_LITERAL_MATCH_CAP - exactNotebooks.size());
    partialNotebooks.stream()
        .filter(nb -> !exactNotebookIds.contains(nb.getId()))
        .limit(remaining)
        .forEach(nb -> notebookHits.add(notebookToHit(nb, 0.9f)));
    return notebookHits;
  }

  private RelationshipLiteralSearchHit notebookToHit(Notebook notebook, float distance) {
    return RelationshipLiteralSearchHit.notebook(notebook.getId(), notebook.getName(), distance);
  }

  private List<Notebook> searchExactNotebookMatches(
      User user, SearchTerm searchTerm, Integer notebookId) {
    String key = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(key)) {
      return List.of();
    }
    if (suppressNotebookLiteralHits(searchTerm, notebookId)) {
      return List.of();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyCircles())) {
      return Stream.concat(
              searchExactNotebooksInMyNotebooksAndSubscriptions(user, key).stream(),
              notebookRepository.searchExactForUserInAllMyCircle(user.getId(), key).stream())
          .toList();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions())) {
      return searchExactNotebooksInMyNotebooksAndSubscriptions(user, key);
    }
    return notebookRepository.searchExactInNotebook(notebookId, key);
  }

  private List<Notebook> searchExactNotebooksInMyNotebooksAndSubscriptions(User user, String key) {
    return Stream.concat(
            notebookRepository.searchExactForUserInAllMyNotebooks(user.getId(), key).stream(),
            notebookRepository.searchExactForUserInAllMySubscriptions(user.getId(), key).stream())
        .toList();
  }

  private List<Notebook> searchPartialNotebookMatches(
      User user, SearchTerm searchTerm, Integer notebookId) {
    String searchKey = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(searchKey)) {
      return List.of();
    }
    if (suppressNotebookLiteralHits(searchTerm, notebookId)) {
      return List.of();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyCircles())) {
      return Stream.concat(
              searchPartialNotebooksInMyNotebooksAndSubscriptions(user, searchTerm).stream(),
              notebookRepository
                  .searchForUserInAllMyCircle(
                      user.getId(), getPattern(searchTerm), getNotebookPageable())
                  .stream())
          .toList();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions())) {
      return searchPartialNotebooksInMyNotebooksAndSubscriptions(user, searchTerm);
    }
    return notebookRepository.searchInNotebook(
        notebookId, getPattern(searchTerm), getNotebookPageable());
  }

  private List<Notebook> searchPartialNotebooksInMyNotebooksAndSubscriptions(
      User user, SearchTerm searchTerm) {
    return Stream.concat(
            notebookRepository
                .searchForUserInAllMyNotebooks(
                    user.getId(), getPattern(searchTerm), getNotebookPageable())
                .stream(),
            notebookRepository
                .searchForUserInAllMySubscriptions(
                    user.getId(), getPattern(searchTerm), getNotebookPageable())
                .stream())
        .toList();
  }

  private Pageable getNotebookPageable() {
    return PageRequest.of(0, NOTEBOOK_LITERAL_MATCH_CAP);
  }

  private List<RelationshipLiteralSearchHit> combineFolderExactAndPartialAsHits(
      List<Folder> exactFolders, List<Folder> partialFolders) {
    List<RelationshipLiteralSearchHit> folderHits = new ArrayList<>();
    for (Folder f : exactFolders) {
      folderHits.add(folderToHit(f, 0.0f));
    }
    java.util.Set<Integer> exactFolderIds =
        exactFolders.stream().map(Folder::getId).collect(Collectors.toSet());
    int remaining = Math.max(0, FOLDER_LITERAL_MATCH_CAP - exactFolders.size());
    partialFolders.stream()
        .filter(f -> !exactFolderIds.contains(f.getId()))
        .limit(remaining)
        .forEach(f -> folderHits.add(folderToHit(f, 0.9f)));
    return folderHits;
  }

  private RelationshipLiteralSearchHit folderToHit(Folder folder, float distance) {
    return RelationshipLiteralSearchHit.folder(
        folder.getId(),
        folder.getName(),
        folder.getNotebook().getId(),
        folder.getNotebook().getName(),
        distance);
  }

  private List<Folder> searchExactFolderMatches(
      User user, SearchTerm searchTerm, Integer notebookId) {
    String key = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(key)) {
      return List.of();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyCircles())) {
      return Stream.concat(
              searchExactFoldersInMyNotebooksAndSubscriptions(user, key).stream(),
              folderRepository.searchExactForUserInAllMyCircle(user.getId(), key).stream())
          .toList();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions())) {
      return searchExactFoldersInMyNotebooksAndSubscriptions(user, key);
    }
    return folderRepository.searchExactInNotebook(notebookId, key);
  }

  private List<Folder> searchExactFoldersInMyNotebooksAndSubscriptions(User user, String key) {
    return Stream.concat(
            folderRepository.searchExactForUserInAllMyNotebooks(user.getId(), key).stream(),
            folderRepository.searchExactForUserInAllMySubscriptions(user.getId(), key).stream())
        .toList();
  }

  private List<Folder> searchPartialFolderMatches(
      User user, SearchTerm searchTerm, Integer notebookId) {
    String searchKey = searchTerm.getTrimmedSearchKey();
    if (Strings.isBlank(searchKey)) {
      return List.of();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyCircles())) {
      return Stream.concat(
              searchPartialFoldersInMyNotebooksAndSubscriptions(user, searchTerm).stream(),
              folderRepository
                  .searchForUserInAllMyCircle(
                      user.getId(), getPattern(searchTerm), getFolderPageable())
                  .stream())
          .toList();
    }
    if (Boolean.TRUE.equals(searchTerm.getAllMyNotebooksAndSubscriptions())) {
      return searchPartialFoldersInMyNotebooksAndSubscriptions(user, searchTerm);
    }
    return folderRepository.searchInNotebook(
        notebookId, getPattern(searchTerm), getFolderPageable());
  }

  private List<Folder> searchPartialFoldersInMyNotebooksAndSubscriptions(
      User user, SearchTerm searchTerm) {
    return Stream.concat(
            folderRepository
                .searchForUserInAllMyNotebooks(
                    user.getId(), getPattern(searchTerm), getFolderPageable())
                .stream(),
            folderRepository
                .searchForUserInAllMySubscriptions(
                    user.getId(), getPattern(searchTerm), getFolderPageable())
                .stream())
        .toList();
  }

  private Pageable getFolderPageable() {
    return PageRequest.of(0, FOLDER_LITERAL_MATCH_CAP);
  }

  private List<RelationshipLiteralSearchHit> sortRelationshipLiteralHits(
      List<RelationshipLiteralSearchHit> hits, Integer notebookId) {
    Comparator<RelationshipLiteralSearchHit> byDistance =
        Comparator.comparing(
            h ->
                h.isNote()
                    ? (h.getNoteSearchResult().getDistance() != null
                        ? h.getNoteSearchResult().getDistance()
                        : Float.MAX_VALUE)
                    : (h.getDistance() != null ? h.getDistance() : Float.MAX_VALUE));
    Comparator<RelationshipLiteralSearchHit> byNotebook =
        (a, b) -> {
          if (notebookId == null) {
            return 0;
          }
          boolean aSame =
              notebookId.equals(
                  a.isNote() ? a.getNoteSearchResult().getNotebookId() : a.getNotebookId());
          boolean bSame =
              notebookId.equals(
                  b.isNote() ? b.getNoteSearchResult().getNotebookId() : b.getNotebookId());
          return Boolean.compare(bSame, aSame);
        };
    Comparator<RelationshipLiteralSearchHit> byLabel =
        Comparator.comparing(
            h ->
                h.isNote()
                    ? (h.getNoteSearchResult().getNoteTopology().getTitle() != null
                        ? h.getNoteSearchResult().getNoteTopology().getTitle()
                        : "")
                    : (h.isFolder()
                        ? (h.getFolderName() != null ? h.getFolderName() : "")
                        : (h.getNotebookName() != null ? h.getNotebookName() : "")),
            String.CASE_INSENSITIVE_ORDER);
    Comparator<RelationshipLiteralSearchHit> byId =
        Comparator.comparing(
            h ->
                h.isNote()
                    ? h.getNoteSearchResult().getNoteTopology().getId()
                    : (h.isFolder() ? h.getFolderId() : h.getNotebookId()));
    return hits.stream()
        .sorted(byDistance.thenComparing(byNotebook).thenComparing(byLabel).thenComparing(byId))
        .toList();
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
    List<NoteSearchResult> results =
        orderedIds.stream()
            .map(id -> idToNote.get(id))
            .filter(java.util.Objects::nonNull)
            .map(n -> noteToSearchResult(n, noteIdToDistance.get(n.getId())))
            .toList();

    return sortByDistanceThenNotebook(results, notebookId);
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
      List<Note> exactMatches, List<Note> partialMatches, Integer avoidNoteId, Integer notebookId) {
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

    return sortByDistanceThenNotebook(results, notebookId);
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
