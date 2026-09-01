package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import com.odde.donut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResolvedWikiLinkTitleResolutionTest {

  @Autowired MakeMe makeMe;
  @Autowired ResolvedWikiLinkService resolvedWikiLinkService;
  @Autowired ResolvedWikiLinkRepository resolvedWikiLinkRepository;

  private List<ResolvedWikiLink> cacheRows(Note carrier) {
    return resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(carrier.getId());
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

    resolvedWikiLinkService.refreshForNote(carrier, user);

    List<ResolvedWikiLink> rows = cacheRows(carrier);
    assertThat(rows, hasSize(2));
    assertThat(
        rows.stream().map(ResolvedWikiLink::getAuthoredLink).toList(),
        containsInAnyOrder("ごろ", "ゴロ"));
    assertThat(
        rows.stream().map(r -> r.getDestinationNote().getId()).toList(),
        containsInAnyOrder(hiraganaTarget.getId(), katakanaTarget.getId()));
  }

  @Test
  void unqualified_link_does_not_resolve_when_same_title_in_different_folders() {
    User user = makeMe.aUser().please();
    Folder folderA = makeMe.aFolder().notebookOwnedBy(user).name("A").please();
    Notebook notebook = folderA.getNotebook();
    Folder folderB = makeMe.aFolder().notebook(notebook).name("B").please();
    makeMe.aNote().title("Dup").folder(folderA).please();
    makeMe.aNote().title("Dup").folder(folderB).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[Dup]]").please();

    resolvedWikiLinkService.refreshForNote(carrier, user);

    assertThat(cacheRows(carrier), empty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "[label](/Pantry/Dup.md)",
        "[label](/Pantry/Dup)",
        "[label](folder/Target.md)",
        "[label](/Title.md)"
      })
  void file_looking_markdown_href_is_not_indexed_as_wiki_link(String content) {
    User user = makeMe.aUser().please();
    Folder recipes = makeMe.aFolder().notebookOwnedBy(user).name("Recipes").please();
    Notebook notebook = recipes.getNotebook();
    Folder pantry = makeMe.aFolder().notebook(notebook).name("Pantry").please();
    makeMe.aNote().title("Dup").folder(recipes).please();
    makeMe.aNote().title("Dup").folder(pantry).please();
    makeMe.aNote().title("Title").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).content(content).please();

    resolvedWikiLinkService.refreshForNote(carrier, user);

    assertThat(cacheRows(carrier), empty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"[[Pantry/Dup]]", "[[Pantry/Dup.md]]", "[[Pantry/Dup|label]]"})
  void folder_path_link_resolves_to_the_note_in_that_folder(String content) {
    User user = makeMe.aUser().please();
    Folder recipes = makeMe.aFolder().notebookOwnedBy(user).name("Recipes").please();
    Notebook notebook = recipes.getNotebook();
    Folder pantry = makeMe.aFolder().notebook(notebook).name("Pantry").please();
    makeMe.aNote().title("Dup").folder(recipes).please();
    Note pantryDup = makeMe.aNote().title("Dup").folder(pantry).please();
    Note carrier = makeMe.aNote().notebook(notebook).content(content).please();

    resolvedWikiLinkService.refreshForNote(carrier, user);

    List<ResolvedWikiLink> rows = cacheRows(carrier);
    assertThat(rows, hasSize(1));
    assertThat(rows.get(0).getDestinationNote().getId(), equalTo(pantryDup.getId()));
  }

  @Test
  void nested_folder_path_link_resolves_to_the_nested_note() {
    User user = makeMe.aUser().please();
    Folder parent = makeMe.aFolder().notebookOwnedBy(user).name("Parent").please();
    Notebook notebook = parent.getNotebook();
    Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();
    makeMe.aNote().title("Title").folder(parent).please();
    Note nested = makeMe.aNote().title("Title").folder(child).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[Parent/Child/Title]]").please();

    resolvedWikiLinkService.refreshForNote(carrier, user);

    List<ResolvedWikiLink> rows = cacheRows(carrier);
    assertThat(rows, hasSize(1));
    assertThat(rows.get(0).getDestinationNote().getId(), equalTo(nested.getId()));
  }

  @Test
  void unqualified_link_does_not_strip_md_suffix_from_title() {
    User user = makeMe.aUser().please();
    Folder folderA = makeMe.aFolder().notebookOwnedBy(user).name("A").please();
    Notebook notebook = folderA.getNotebook();
    makeMe.aNote().title("Dup").folder(folderA).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[Dup.md]]").please();

    resolvedWikiLinkService.refreshForNote(carrier, user);

    assertThat(cacheRows(carrier), empty());
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

    resolvedWikiLinkService.refreshForNote(carrier, user);

    assertThat(cacheRows(carrier).get(0).getDestinationNote().getId(), equalTo(voiced.getId()));
  }

  @Test
  void unqualified_link_does_not_resolve_voiced_target_to_unvoiced_title() {
    User user = makeMe.aUser().please();
    Note unvoiced = makeMe.aNote().title("ころ").notebookOwnedBy(user).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(unvoiced).content("[[ごろ]]").please();

    resolvedWikiLinkService.refreshForNote(carrier, user);

    assertThat(cacheRows(carrier), empty());
  }
}
