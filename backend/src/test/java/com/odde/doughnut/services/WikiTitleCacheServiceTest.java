package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.RelationType;
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
      Note source = makeMe.aNote().title("Alpha").under(root).please();
      Note target = makeMe.aNote().title("Beta").under(root).please();
      String details =
          RelationshipNoteMarkdownFormatter.format(
              RelationType.RELATED_TO, source.getTitle(), target.getTitle(), null);
      Note carrier = makeMe.aNote().under(root).details(details).please();

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
      Note a = makeMe.aNote().title("OnlyA").under(root).please();
      Note b = makeMe.aNote().title("OnlyB").under(root).please();
      Note carrier = makeMe.aNote().under(root).details("[[OnlyA]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);
      assertThat(
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), hasSize(1));

      carrier.setDetails("[[OnlyB]]");
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
      Note shared = makeMe.aNote().title("Same").under(root).please();
      Note carrier = makeMe.aNote().under(root).details("[[Same]] and again [[Same]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(shared.getId()));
    }

    @Test
    void unqualified_link_picks_lowest_note_id_when_duplicate_titles_exist_in_subtree() {
      User user = makeMe.aUser().please();
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      Note firstCreated = makeMe.aNote().title("Dup").under(root).please();
      makeMe.aNote().title("Dup").under(root).please();
      Note carrier = makeMe.aNote().under(root).details("[[Dup]]").please();

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
      makeMe.aNote().title("Hidden Note").under(headSecret).please();

      User viewer = makeMe.aUser().please();
      Note headSource = makeMe.aNote().creatorAndOwner(viewer).title("My Notebook").please();
      Note carrier =
          makeMe.aNote().under(headSource).details("Try [[Secret Notebook:Hidden Note]].").please();

      wikiTitleCacheService.refreshForNote(carrier, viewer);

      assertThat(noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), empty());
    }

    @Test
    void clears_rows_when_details_become_blank() {
      User user = makeMe.aUser().please();
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      Note a = makeMe.aNote().title("A").under(root).please();
      Note carrier = makeMe.aNote().under(root).details("[[A]]").please();

      wikiTitleCacheService.refreshForNote(carrier, user);
      assertThat(
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), hasSize(1));

      carrier.setDetails("   ");
      makeMe.entityPersister.merge(carrier);
      wikiTitleCacheService.refreshForNote(carrier, user);

      assertThat(noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), empty());
    }
  }

  @Test
  void references_notes_for_viewer_orders_referrers_by_note_id() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note second = makeMe.aNote().under(root).details("[[Focal]]").please();
    Note first = makeMe.aNote().under(root).details("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(first, user);
    wikiTitleCacheService.refreshForNote(second, user);

    List<Note> refs = wikiTitleCacheService.referencesNotesForViewer(focal, user);

    assertThat(refs, hasSize(2));
    assertThat(refs.get(0).getId(), equalTo(Math.min(first.getId(), second.getId())));
    assertThat(refs.get(1).getId(), equalTo(Math.max(first.getId(), second.getId())));
  }
}
