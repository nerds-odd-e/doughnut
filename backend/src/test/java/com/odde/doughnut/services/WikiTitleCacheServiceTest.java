package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.controllers.dto.WikiTitle;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiTitleCacheServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired WikiTitleCacheService wikiTitleCacheService;
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;

  @Nested
  class refreshForNote {

    @Test
    void stores_resolved_links_from_relationship_frontmatter_and_body() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note source = makeMe.aNote().title("Alpha").inNotebook(notebook).please();
      Note target = makeMe.aNote().title("Beta").inNotebook(notebook).please();
      String markdown =
          RelationshipNoteMarkdownFormatter.format(
              "related to", source.getTitle(), target.getTitle(), null);
      Note carrier = makeMe.aNote().inNotebook(notebook).content(markdown).please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(2));
      assertThat(rows.get(0).getLinkText(), equalTo("Alpha"));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(source.getId()));
      assertThat(rows.get(1).getLinkText(), equalTo("Beta"));
      assertThat(rows.get(1).getTargetNote().getId(), equalTo(target.getId()));
    }

    @Test
    void replaces_previous_rows_on_second_refresh() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note a = makeMe.aNote().title("OnlyA").inNotebook(notebook).please();
      Note b = makeMe.aNote().title("OnlyB").inNotebook(notebook).please();
      Note carrier = makeMe.aNote().inNotebook(notebook).content("[[OnlyA]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);
      assertThat(
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), hasSize(1));

      carrier.setContent("[[OnlyB]]");
      makeMe.entityPersister.merge(carrier);
      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getLinkText(), equalTo("OnlyB"));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(b.getId()));
    }

    @Test
    void dedupes_duplicate_link_text_in_order_of_first_occurrence() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note shared = makeMe.aNote().title("Same").inNotebook(notebook).please();
      Note carrier =
          makeMe.aNote().inNotebook(notebook).content("[[Same]] and again [[Same]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(shared.getId()));
    }

    @Test
    void keeps_distinct_cache_rows_when_link_spellings_collide_under_unicode_ci() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Folder folderA = makeMe.aFolder().notebook(notebook).name("HiraFolder").please();
      Folder folderB = makeMe.aFolder().notebook(notebook).name("KataFolder").please();
      Note hiraganaTarget = makeMe.aNote().title("ごろ").folder(folderA).please();
      Note katakanaTarget = makeMe.aNote().title("ゴロ").folder(folderB).please();
      Note carrier = makeMe.aNote().inNotebook(notebook).content("[[ごろ]] [[ゴロ]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(2));
      assertThat(
          rows.stream().map(NoteWikiTitleCache::getLinkText).toList(),
          containsInAnyOrder("ごろ", "ゴロ"));
      assertThat(
          rows.stream().map(r -> r.getTargetNote().getId()).toList(),
          containsInAnyOrder(hiraganaTarget.getId(), katakanaTarget.getId()));
    }

    @Test
    void multiple_display_labels_to_same_target_keep_separate_cache_rows_and_wiki_titles() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note shared = makeMe.aNote().title("Same").inNotebook(notebook).please();
      Note carrier =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .content("[[Same|first label]] and [[Same|second label]]")
              .please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(2));
      assertThat(rows.get(0).getLinkText(), equalTo("Same|first label"));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(shared.getId()));
      assertThat(rows.get(1).getLinkText(), equalTo("Same|second label"));
      assertThat(rows.get(1).getTargetNote().getId(), equalTo(shared.getId()));

      List<WikiTitle> titles = wikiTitleCacheService.wikiTitlesForViewer(carrier, user);
      assertThat(titles, hasSize(2));
      assertThat(titles.get(0).getTargetToken(), equalTo("Same"));
      assertThat(titles.get(0).getDisplayText(), equalTo("first label"));
      assertThat(titles.get(1).getTargetToken(), equalTo("Same"));
      assertThat(titles.get(1).getDisplayText(), equalTo("second label"));
    }

    @Test
    void outgoing_targets_dedupe_by_resolved_note_id_across_display_text_variants() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe.aNote().title("Same").inNotebook(notebook).please();
      Note carrier = makeMe.aNote().inNotebook(notebook).content("[[Same|a]] [[Same|b]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<Note> outgoing =
          wikiTitleCacheService.outgoingWikiLinkTargetNotesForViewer(carrier, user);
      assertThat(outgoing, hasSize(1));
      assertThat(outgoing.get(0).getTitle(), equalTo("Same"));
    }

    @Test
    void unqualified_link_picks_lowest_note_id_when_same_title_in_different_folders() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Folder folderA = makeMe.aFolder().notebook(notebook).name("A").please();
      Folder folderB = makeMe.aFolder().notebook(notebook).name("B").please();
      Note firstCreated = makeMe.aNote().title("Dup").folder(folderA).please();
      makeMe.aNote().title("Dup").folder(folderB).please();
      Note carrier = makeMe.aNote().inNotebook(notebook).content("[[Dup]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(firstCreated.getId()));
    }

    @Test
    void unqualified_link_distinguishes_unvoiced_and_voiced_hiragana_title_spellings() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Folder folderA = makeMe.aFolder().notebook(notebook).name("KoroFolder").please();
      Folder folderB = makeMe.aFolder().notebook(notebook).name("GoroFolder").please();
      makeMe.aNote().title("ころ").folder(folderA).please();
      Note voiced = makeMe.aNote().title("ごろ").folder(folderB).please();
      Note carrier = makeMe.aNote().inNotebook(notebook).content("[[ごろ]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(voiced.getId()));
    }

    @Test
    void unqualified_link_does_not_resolve_voiced_target_to_unvoiced_title() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe.aNote().title("ころ").inNotebook(notebook).please();
      Note carrier = makeMe.aNote().inNotebook(notebook).content("[[ごろ]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      assertThat(noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), empty());
    }

    @Test
    void omits_qualified_link_when_target_notebook_is_not_readable() {
      User otherUser = makeMe.aUser().please();
      Notebook secretNb =
          makeMe.aNotebook().creatorAndOwner(otherUser).name("Secret Notebook").please();
      makeMe.aNote().title("Hidden Note").inNotebook(secretNb).please();

      User viewer = makeMe.aUser().please();
      Notebook viewerNb = makeMe.aNotebook().creatorAndOwner(viewer).name("My Notebook").please();
      Note carrier =
          makeMe
              .aNote()
              .inNotebook(viewerNb)
              .content("Try [[Secret Notebook:Hidden Note]].")
              .please();

      wikiTitleCacheService.refreshForNote(carrier, viewer);

      assertThat(noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), empty());
    }

    @Test
    void clears_rows_when_content_becomes_blank() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note a = makeMe.aNote().title("A").inNotebook(notebook).please();
      Note carrier = makeMe.aNote().inNotebook(notebook).content("[[A]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);
      assertThat(
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), hasSize(1));

      carrier.setContent("   ");
      makeMe.entityPersister.merge(carrier);
      wikiTitleCacheService.refreshForNote(carrier, user);

      assertThat(noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), empty());
    }
  }

  @Test
  void references_notes_for_viewer_orders_referrers_by_note_id() {
    User user = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").inNotebook(notebook).please();
    Note second = makeMe.aNote().inNotebook(notebook).content("[[Focal]]").please();
    Note first = makeMe.aNote().inNotebook(notebook).content("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(first, user);
    wikiTitleCacheService.refreshForNote(second, user);

    List<Note> refs = wikiTitleCacheService.referencesNotesForViewer(focal, user);

    assertThat(refs, hasSize(2));
    assertThat(refs.get(0).getId(), equalTo(Math.min(first.getId(), second.getId())));
    assertThat(refs.get(1).getId(), equalTo(Math.max(first.getId(), second.getId())));
  }

  @Test
  void references_notes_for_viewer_includes_notebook_root_referrer_linking_to_descendant() {
    User user = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").inNotebook(notebook).please();
    Note referrerAtNotebookRoot = makeMe.aNote().inNotebook(notebook).content("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(referrerAtNotebookRoot, user);

    List<Note> refs = wikiTitleCacheService.referencesNotesForViewer(focal, user);

    assertThat(refs, hasItem(referrerAtNotebookRoot));
  }
}
