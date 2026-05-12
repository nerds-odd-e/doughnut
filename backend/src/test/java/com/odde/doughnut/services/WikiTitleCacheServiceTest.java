package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.controllers.dto.WikiTitle;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
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
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      Note source = makeMe.aNote().title("Alpha").underSameNotebookAs(root).please();
      Note target = makeMe.aNote().title("Beta").underSameNotebookAs(root).please();
      String markdown =
          RelationshipNoteMarkdownFormatter.format(
              "related to", source.getTitle(), target.getTitle(), null);
      Note carrier = makeMe.aNote().underSameNotebookAs(root).content(markdown).please();

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
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      Note a = makeMe.aNote().title("OnlyA").underSameNotebookAs(root).please();
      Note b = makeMe.aNote().title("OnlyB").underSameNotebookAs(root).please();
      Note carrier = makeMe.aNote().underSameNotebookAs(root).content("[[OnlyA]]").please();

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
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      Note shared = makeMe.aNote().title("Same").underSameNotebookAs(root).please();
      Note carrier =
          makeMe.aNote().underSameNotebookAs(root).content("[[Same]] and again [[Same]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(shared.getId()));
    }

    @Test
    void multiple_display_labels_to_same_target_keep_separate_cache_rows_and_wiki_titles() {
      User user = makeMe.aUser().please();
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      Note shared = makeMe.aNote().title("Same").underSameNotebookAs(root).please();
      Note carrier =
          makeMe
              .aNote()
              .underSameNotebookAs(root)
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
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      makeMe.aNote().title("Same").underSameNotebookAs(root).please();
      Note carrier =
          makeMe.aNote().underSameNotebookAs(root).content("[[Same|a]] [[Same|b]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<Note> outgoing =
          wikiTitleCacheService.outgoingWikiLinkTargetNotesForViewer(carrier, user);
      assertThat(outgoing, hasSize(1));
      assertThat(outgoing.get(0).getTitle(), equalTo("Same"));
    }

    @Test
    void unqualified_link_picks_lowest_note_id_when_same_title_in_different_folders() {
      User user = makeMe.aUser().please();
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      Folder folderA = makeMe.aFolder().notebook(root.getNotebook()).name("A").please();
      Folder folderB = makeMe.aFolder().notebook(root.getNotebook()).name("B").please();
      Note firstCreated =
          makeMe.aNote().title("Dup").underSameNotebookAs(root).folder(folderA).please();
      makeMe.aNote().title("Dup").underSameNotebookAs(root).folder(folderB).please();
      Note carrier = makeMe.aNote().underSameNotebookAs(root).content("[[Dup]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(firstCreated.getId()));
    }

    @Test
    void omits_qualified_link_when_target_notebook_is_not_readable() {
      User otherUser = makeMe.aUser().please();
      Note headSecret = makeMe.aNote().creatorAndOwner(otherUser).title("Secret Notebook").please();
      makeMe.aNote().title("Hidden Note").underSameNotebookAs(headSecret).please();

      User viewer = makeMe.aUser().please();
      Note headSource = makeMe.aNote().creatorAndOwner(viewer).title("My Notebook").please();
      Note carrier =
          makeMe
              .aNote()
              .underSameNotebookAs(headSource)
              .content("Try [[Secret Notebook:Hidden Note]].")
              .please();

      wikiTitleCacheService.refreshForNote(carrier, viewer);

      assertThat(noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), empty());
    }

    @Test
    void clears_rows_when_content_becomes_blank() {
      User user = makeMe.aUser().please();
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      Note a = makeMe.aNote().title("A").underSameNotebookAs(root).please();
      Note carrier = makeMe.aNote().underSameNotebookAs(root).content("[[A]]").please();

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
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal =
        makeMe.aNote().title("Focal").underSameNotebookAs(root).creatorAndOwner(user).please();
    Note second =
        makeMe
            .aNote()
            .underSameNotebookAs(root)
            .creatorAndOwner(user)
            .content("[[Focal]]")
            .please();
    Note first =
        makeMe
            .aNote()
            .underSameNotebookAs(root)
            .creatorAndOwner(user)
            .content("[[Focal]]")
            .please();
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
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal =
        makeMe.aNote().title("Focal").underSameNotebookAs(root).creatorAndOwner(user).please();
    Note referrerAtNotebookRoot =
        makeMe.aNote().underSameNotebookAs(root).content("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(referrerAtNotebookRoot, user);

    List<Note> refs = wikiTitleCacheService.referencesNotesForViewer(focal, user);

    assertThat(refs, hasItem(referrerAtNotebookRoot));
  }
}
