package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResolvedWikiLinkServiceRefreshNotebookScopeTest {

  @Autowired MakeMe makeMe;
  @Autowired ResolvedWikiLinkService resolvedWikiLinkService;
  @Autowired ResolvedWikiLinkRepository resolvedWikiLinkRepository;

  private List<ResolvedWikiLink> cacheRows(Note carrier) {
    return resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(carrier.getId());
  }

  @Test
  void reresolves_other_notes_whose_shorthand_cardinality_changed_elsewhere_in_the_notebook() {
    User user = makeMe.aUser().please();
    Folder folderA = makeMe.aFolder().notebookOwnedBy(user).name("A").please();
    Notebook notebook = folderA.getNotebook();
    Folder folderB = makeMe.aFolder().notebook(notebook).name("B").please();
    Note target = makeMe.aNote().title("Target").folder(folderA).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[Target]]").please();

    resolvedWikiLinkService.refreshForNote(carrier, user);
    List<ResolvedWikiLink> resolvedRows = cacheRows(carrier);
    assertThat(resolvedRows, hasSize(1));
    assertThat(resolvedRows.get(0).getDestinationNote().getId(), equalTo(target.getId()));

    // A second note with the same title in another folder of the same notebook makes
    // carrier's shorthand ambiguous, but carrier itself was never touched, so its
    // resolved-link row is now stale.
    makeMe.aNote().title("Target").folder(folderB).please();
    assertThat(cacheRows(carrier), hasSize(1));

    resolvedWikiLinkService.refreshNotebookScope(notebook, user);

    assertThat(cacheRows(carrier), empty());
  }

  @Test
  void rebuilds_every_notes_alias_index_before_resolving_any_notes_links() {
    User user = makeMe.aUser().please();
    Note target = makeMe.aNote().title("Target").notebookOwnedBy(user).please();
    Note referrer = makeMe.aNote().underSameNotebookAs(target).content("[[Target]]").please();
    Notebook notebook = target.getNotebook();

    resolvedWikiLinkService.refreshForNote(referrer, user);
    assertThat(cacheRows(referrer), hasSize(1));

    // Higher id than referrer; its alias frontmatter is persisted directly (bypassing the
    // controller) so its NoteAliasIndex row does not exist yet when refreshNotebookScope runs.
    makeMe
        .aNote()
        .underSameNotebookAs(target)
        .title("C")
        .content("---\naliases:\n  - Target\n---\n\nBody text")
        .please();

    resolvedWikiLinkService.refreshNotebookScope(notebook, user);

    assertThat(cacheRows(referrer), empty());
  }
}
