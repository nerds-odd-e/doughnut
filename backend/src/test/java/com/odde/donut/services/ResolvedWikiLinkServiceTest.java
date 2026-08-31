package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteAliasIndex;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteAliasIndexRepository;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import com.odde.donut.testability.MakeMe;
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
class ResolvedWikiLinkServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired ResolvedWikiLinkService resolvedWikiLinkService;
  @Autowired ResolvedWikiLinkRepository resolvedWikiLinkRepository;
  @Autowired NoteAliasIndexRepository noteAliasIndexRepository;

  private List<ResolvedWikiLink> cacheRows(Note carrier) {
    return resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(carrier.getId());
  }

  @Nested
  class refreshForNote {

    @Test
    void stores_resolved_links_from_relationship_frontmatter() {
      User user = makeMe.aUser().please();
      Note source = makeMe.aNote().title("Alpha").notebookOwnedBy(user).please();
      Note target = makeMe.aNote().title("Beta").underSameNotebookAs(source).please();
      Note carrier =
          makeMe
              .aNote()
              .underSameNotebookAs(source)
              .withWikiLinksInFrontmatter(source, target)
              .please();

      resolvedWikiLinkService.refreshForNote(carrier, user);

      List<ResolvedWikiLink> rows = cacheRows(carrier);
      assertThat(rows, hasSize(2));
      assertThat(rows.get(0).getAuthoredLink(), equalTo("Alpha"));
      assertThat(rows.get(0).getDestinationNote().getId(), equalTo(source.getId()));
      assertThat(rows.get(1).getAuthoredLink(), equalTo("Beta"));
      assertThat(rows.get(1).getDestinationNote().getId(), equalTo(target.getId()));
    }

    @Test
    void replaces_previous_rows_on_second_refresh() {
      User user = makeMe.aUser().please();
      Note onlyA = makeMe.aNote().title("OnlyA").notebookOwnedBy(user).please();
      Note onlyB = makeMe.aNote().title("OnlyB").underSameNotebookAs(onlyA).please();
      Note carrier = makeMe.aNote().underSameNotebookAs(onlyA).content("[[OnlyA]]").please();

      resolvedWikiLinkService.refreshForNote(carrier, user);
      assertThat(cacheRows(carrier), hasSize(1));

      carrier.setContent("[[OnlyB]]");
      makeMe.entityPersister.merge(carrier);
      resolvedWikiLinkService.refreshForNote(carrier, user);

      List<ResolvedWikiLink> rows = cacheRows(carrier);
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getAuthoredLink(), equalTo("OnlyB"));
      assertThat(rows.get(0).getDestinationNote().getId(), equalTo(onlyB.getId()));
    }

    @Test
    void dedupes_duplicate_link_text_in_order_of_first_occurrence() {
      User user = makeMe.aUser().please();
      Note shared = makeMe.aNote().title("Same").notebookOwnedBy(user).please();
      Note carrier =
          makeMe
              .aNote()
              .underSameNotebookAs(shared)
              .content("[[Same]] and again [[Same]]")
              .please();

      resolvedWikiLinkService.refreshForNote(carrier, user);

      List<ResolvedWikiLink> rows = cacheRows(carrier);
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getDestinationNote().getId(), equalTo(shared.getId()));
    }

    @Test
    void unqualified_link_resolves_title_case_insensitively() {
      User user = makeMe.aUser().please();
      Note target = makeMe.aNote().title("MixedCase").notebookOwnedBy(user).please();
      Note carrier = makeMe.aNote().underSameNotebookAs(target).content("[[mixedcase]]").please();

      resolvedWikiLinkService.refreshForNote(carrier, user);

      List<ResolvedWikiLink> rows = cacheRows(carrier);
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getAuthoredLink(), equalTo("mixedcase"));
      assertThat(rows.get(0).getDestinationNote().getId(), equalTo(target.getId()));
    }

    @Test
    void qualified_link_resolves_notebook_and_title_case_insensitively() {
      User user = makeMe.aUser().please();
      Notebook targetNotebook = makeMe.aNotebook().creatorAndOwner(user).name("MyBook").please();
      Note target = makeMe.aNote().title("MixedCase").notebook(targetNotebook).please();
      Notebook sourceNotebook = makeMe.aNotebook().creatorAndOwner(user).name("Source").please();
      Note carrier =
          makeMe.aNote().notebook(sourceNotebook).content("[[mybook:MIXEDCASE]]").please();

      resolvedWikiLinkService.refreshForNote(carrier, user);

      List<ResolvedWikiLink> rows = cacheRows(carrier);
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getAuthoredLink(), equalTo("mybook:MIXEDCASE"));
      assertThat(rows.get(0).getDestinationNote().getId(), equalTo(target.getId()));
    }

    @Test
    void dedupes_link_text_that_differs_only_by_case() {
      User user = makeMe.aUser().please();
      Note shared = makeMe.aNote().title("Same").notebookOwnedBy(user).please();
      Note carrier =
          makeMe
              .aNote()
              .underSameNotebookAs(shared)
              .content("[[Same]] and again [[same]]")
              .please();

      resolvedWikiLinkService.refreshForNote(carrier, user);

      List<ResolvedWikiLink> rows = cacheRows(carrier);
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getAuthoredLink(), equalTo("Same"));
      assertThat(rows.get(0).getDestinationNote().getId(), equalTo(shared.getId()));
    }

    @Test
    void omits_qualified_link_when_target_notebook_is_not_readable() {
      User otherUser = makeMe.aUser().please();
      Notebook secretNb =
          makeMe.aNotebook().creatorAndOwner(otherUser).name("Secret Notebook").please();
      makeMe.aNote().title("Hidden Note").notebook(secretNb).please();

      User viewer = makeMe.aUser().please();
      Notebook viewerNb = makeMe.aNotebook().creatorAndOwner(viewer).name("My Notebook").please();
      Note carrier =
          makeMe
              .aNote()
              .notebook(viewerNb)
              .content("Try [[Secret Notebook:Hidden Note]].")
              .please();

      resolvedWikiLinkService.refreshForNote(carrier, viewer);

      assertThat(cacheRows(carrier), empty());
    }

    @Test
    void refresh_populates_alias_index_and_resolves_unambiguous_alias_links() {
      User user = makeMe.aUser().please();
      Note target = makeMe.aNote().title("colour").notebookOwnedBy(user).aliases("color").please();
      Note carrier = makeMe.aNote().underSameNotebookAs(target).content("see [[color]]").please();

      resolvedWikiLinkService.refreshForNote(target, user);
      resolvedWikiLinkService.refreshForNote(carrier, user);

      List<NoteAliasIndex> aliasRows =
          noteAliasIndexRepository.findByNote_IdOrderByIdAsc(target.getId());
      assertThat(aliasRows, hasSize(1));
      assertThat(aliasRows.get(0).getAliasDisplay(), equalTo("color"));

      List<ResolvedWikiLink> rows = cacheRows(carrier);
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getDestinationNote().getId(), equalTo(target.getId()));
    }

    @Test
    void clears_rows_when_content_becomes_blank() {
      User user = makeMe.aUser().please();
      Note target = makeMe.aNote().title("A").notebookOwnedBy(user).please();
      Note carrier = makeMe.aNote().underSameNotebookAs(target).content("[[A]]").please();

      resolvedWikiLinkService.refreshForNote(carrier, user);
      assertThat(cacheRows(carrier), hasSize(1));

      carrier.setContent("   ");
      makeMe.entityPersister.merge(carrier);
      resolvedWikiLinkService.refreshForNote(carrier, user);

      assertThat(cacheRows(carrier), empty());
    }
  }
}
