package com.odde.doughnut.services.focusContext;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class FocusContextRetrievalTestBase {

  /**
   * Combined content budget that leaves the post-focus remainder below {@link
   * FocusContextConstants#MIN_RELATED_TOKENS_FOR_FOLDER_PEER_CONTEXT}, so folder peers are omitted
   * while wiki BFS still receives the full remainder.
   */
  static final int CONTENT_BUDGET_WITHOUT_FOLDER_PEERS =
      FocusContextConstants.MIN_RELATED_TOKENS_FOR_FOLDER_PEER_CONTEXT - 1;

  @Autowired MakeMe makeMe;
  @Autowired FocusContextRetrievalService service;
  @Autowired WikiTitleCacheService wikiTitleCacheService;

  void refreshWikiCache(Note note, User viewer) {
    wikiTitleCacheService.refreshForNote(note, viewer);
  }

  /** Folder peer: retrieval path ends at the anchor ({@code size == depth}). */
  static boolean isFolderPeer(FocusContextNote note) {
    return note.getRetrievalPath().size() == note.getDepth();
  }

  /** Wiki-reached: retrieval path ends at the note ({@code size == depth + 1}). */
  static boolean isWikiReached(FocusContextNote note) {
    return note.getRetrievalPath().size() == note.getDepth() + 1;
  }

  static List<String> folderPeerTitles(FocusContextResult result) {
    return result.getRelatedNotes().stream()
        .filter(FocusContextRetrievalTestBase::isFolderPeer)
        .map(FocusContextNote::getTitle)
        .toList();
  }

  static List<String> wikiReachedTitles(FocusContextResult result) {
    return result.getRelatedNotes().stream()
        .filter(FocusContextRetrievalTestBase::isWikiReached)
        .map(FocusContextNote::getTitle)
        .toList();
  }

  static List<String> relatedTitles(FocusContextResult result) {
    return result.getRelatedNotes().stream().map(FocusContextNote::getTitle).toList();
  }

  static FocusContextNote relatedByTitle(FocusContextResult result, String title) {
    return result.getRelatedNotes().stream()
        .filter(n -> title.equals(n.getTitle()))
        .findFirst()
        .orElseThrow();
  }
}
