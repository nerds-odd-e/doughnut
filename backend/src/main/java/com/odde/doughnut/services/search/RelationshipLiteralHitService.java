package com.odde.doughnut.services.search;

import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.RelationshipLiteralSearchHit;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import java.util.ArrayList;
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
public class RelationshipLiteralHitService {
  private static final int FOLDER_LITERAL_MATCH_CAP = 12;
  private static final int NOTEBOOK_LITERAL_MATCH_CAP = 12;

  private final FolderRepository folderRepository;
  private final NotebookRepository notebookRepository;
  private final RelationshipLiteralHitSorter relationshipLiteralHitSorter;

  public RelationshipLiteralHitService(
      FolderRepository folderRepository,
      NotebookRepository notebookRepository,
      RelationshipLiteralHitSorter relationshipLiteralHitSorter) {
    this.folderRepository = folderRepository;
    this.notebookRepository = notebookRepository;
    this.relationshipLiteralHitSorter = relationshipLiteralHitSorter;
  }

  public List<RelationshipLiteralSearchHit> mergeNoteAndContainerLiteralHits(
      User user, SearchTerm searchTerm, Integer notebookId, List<NoteSearchResult> noteResults) {
    List<RelationshipLiteralSearchHit> hits = new ArrayList<>();
    noteResults.forEach(r -> hits.add(RelationshipLiteralSearchHit.note(r)));
    List<Folder> exactFolders = searchExactFolderMatches(user, searchTerm, notebookId);
    List<Folder> partialFolders = searchPartialFolderMatches(user, searchTerm, notebookId);
    hits.addAll(combineFolderExactAndPartialAsHits(exactFolders, partialFolders));
    List<Notebook> exactNotebooks = searchExactNotebookMatches(user, searchTerm, notebookId);
    List<Notebook> partialNotebooks = searchPartialNotebookMatches(user, searchTerm, notebookId);
    hits.addAll(combineNotebookExactAndPartialAsHits(exactNotebooks, partialNotebooks));
    return relationshipLiteralHitSorter.sort(hits, notebookId);
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
    exactNotebooks.forEach(nb -> notebookHits.add(notebookToHit(nb, 0.0f)));
    Set<Integer> exactNotebookIds =
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
    if (Strings.isBlank(key) || suppressNotebookLiteralHits(searchTerm, notebookId)) {
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
    if (Strings.isBlank(searchKey) || suppressNotebookLiteralHits(searchTerm, notebookId)) {
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

  private List<RelationshipLiteralSearchHit> combineFolderExactAndPartialAsHits(
      List<Folder> exactFolders, List<Folder> partialFolders) {
    List<RelationshipLiteralSearchHit> folderHits = new ArrayList<>();
    exactFolders.forEach(f -> folderHits.add(folderToHit(f, 0.0f)));
    Set<Integer> exactFolderIds =
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

  private Pageable getNotebookPageable() {
    return PageRequest.of(0, NOTEBOOK_LITERAL_MATCH_CAP);
  }

  private Pageable getFolderPageable() {
    return PageRequest.of(0, FOLDER_LITERAL_MATCH_CAP);
  }

  private String getPattern(SearchTerm searchTerm) {
    return "%" + searchTerm.getTrimmedSearchKey() + "%";
  }
}
