package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiTitleCacheTitleResolutionTest {

  @Autowired MakeMe makeMe;
  @Autowired WikiTitleCacheService wikiTitleCacheService;
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;

  private List<NoteWikiTitleCache> cacheRows(Note carrier) {
    return noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
  }

  @Test
  void keeps_distinct_cache_rows_when_link_spellings_collide_under_unicode_ci() {
    User user = makeMe.aUser().please();
    Folder folderA = makeMe.aFolder().notebookOwnedBy(user).name("HiraFolder").please();
    Notebook notebook = folderA.getNotebook();
    Folder folderB = makeMe.aFolder().notebook(notebook).name("KataFolder").please();
    Note hiraganaTarget = makeMe.aNote().title("ごろ").folder(folderA).please();
    Note katakanaTarget = makeMe.aNote().title("ゴロ").folder(folderB).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[ごろ]] [[ゴロ]]").please();

    wikiTitleCacheService.refreshForNote(carrier, user);

    List<NoteWikiTitleCache> rows = cacheRows(carrier);
    assertThat(rows, hasSize(2));
    assertThat(
        rows.stream().map(NoteWikiTitleCache::getLinkText).toList(),
        containsInAnyOrder("ごろ", "ゴロ"));
    assertThat(
        rows.stream().map(r -> r.getTargetNote().getId()).toList(),
        containsInAnyOrder(hiraganaTarget.getId(), katakanaTarget.getId()));
  }

  @Test
  void unqualified_link_picks_lowest_note_id_when_same_title_in_different_folders() {
    User user = makeMe.aUser().please();
    Folder folderA = makeMe.aFolder().notebookOwnedBy(user).name("A").please();
    Notebook notebook = folderA.getNotebook();
    Folder folderB = makeMe.aFolder().notebook(notebook).name("B").please();
    Note firstCreated = makeMe.aNote().title("Dup").folder(folderA).please();
    makeMe.aNote().title("Dup").folder(folderB).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[Dup]]").please();

    wikiTitleCacheService.refreshForNote(carrier, user);

    List<NoteWikiTitleCache> rows = cacheRows(carrier);
    assertThat(rows, hasSize(1));
    assertThat(rows.get(0).getTargetNote().getId(), equalTo(firstCreated.getId()));
  }

  @Test
  void unqualified_link_distinguishes_unvoiced_and_voiced_hiragana_title_spellings() {
    User user = makeMe.aUser().please();
    Folder folderA = makeMe.aFolder().notebookOwnedBy(user).name("KoroFolder").please();
    Notebook notebook = folderA.getNotebook();
    Folder folderB = makeMe.aFolder().notebook(notebook).name("GoroFolder").please();
    makeMe.aNote().title("ころ").folder(folderA).please();
    Note voiced = makeMe.aNote().title("ごろ").folder(folderB).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[ごろ]]").please();

    wikiTitleCacheService.refreshForNote(carrier, user);

    assertThat(cacheRows(carrier).get(0).getTargetNote().getId(), equalTo(voiced.getId()));
  }

  @Test
  void unqualified_link_does_not_resolve_voiced_target_to_unvoiced_title() {
    User user = makeMe.aUser().please();
    Note unvoiced = makeMe.aNote().title("ころ").notebookOwnedBy(user).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(unvoiced).content("[[ごろ]]").please();

    wikiTitleCacheService.refreshForNote(carrier, user);

    assertThat(cacheRows(carrier), empty());
  }
}
