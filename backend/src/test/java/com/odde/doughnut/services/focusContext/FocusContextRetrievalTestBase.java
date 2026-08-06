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

  @Autowired MakeMe makeMe;
  @Autowired FocusContextRetrievalService service;
  @Autowired WikiTitleCacheService wikiTitleCacheService;

  void refreshWikiCache(Note note, User viewer) {
    wikiTitleCacheService.refreshForNote(note, viewer);
  }

  static List<String> folderSiblingTitles(FocusContextResult result) {
    return result.getRelatedNotes().stream()
        .filter(n -> n.getEdgeType() == FocusContextEdgeType.FolderSibling)
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
