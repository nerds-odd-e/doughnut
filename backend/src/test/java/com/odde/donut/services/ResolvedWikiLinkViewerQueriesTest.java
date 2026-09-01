package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
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
class ResolvedWikiLinkViewerQueriesTest {

  @Autowired MakeMe makeMe;
  @Autowired ResolvedWikiLinkService resolvedWikiLinkService;
  @Autowired ResolvedWikiLinkRepository resolvedWikiLinkRepository;

  private List<ResolvedWikiLink> cacheRows(Note carrier) {
    return resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(carrier.getId());
  }

  @Test
  void multiple_display_labels_to_same_target_keep_separate_cache_rows_and_wiki_titles() {
    User user = makeMe.aUser().please();
    Note shared = makeMe.aNote().title("Same").notebookOwnedBy(user).please();
    Note carrier =
        makeMe
            .aNote()
            .underSameNotebookAs(shared)
            .content("[[Same|first label]] and [[Same|second label]]")
            .please();

    resolvedWikiLinkService.refreshForNote(carrier, user);

    List<ResolvedWikiLink> rows = cacheRows(carrier);
    assertThat(rows, hasSize(2));
    assertThat(rows.get(0).getAuthoredLink(), equalTo("Same|first label"));
    assertThat(rows.get(0).getDestinationNote().getId(), equalTo(shared.getId()));
    assertThat(rows.get(1).getAuthoredLink(), equalTo("Same|second label"));
    assertThat(rows.get(1).getDestinationNote().getId(), equalTo(shared.getId()));

    List<WikiLink> titles = resolvedWikiLinkService.wikiLinksForViewer(carrier, user);
    assertThat(titles, hasSize(2));
    assertThat(titles.get(0).getDisplayText(), equalTo("first label"));
    assertThat(titles.get(1).getDisplayText(), equalTo("second label"));
  }

  @Test
  void file_looking_markdown_href_does_not_appear_in_viewer_wiki_links() {
    User user = makeMe.aUser().please();
    Folder folder = makeMe.aFolder().notebookOwnedBy(user).name("Folder").please();
    makeMe.aNote().title("Title").folder(folder).please();
    Note carrier =
        makeMe.aNote().notebook(folder.getNotebook()).content("[label](/Folder/Title.md)").please();

    resolvedWikiLinkService.refreshForNote(carrier, user);

    assertThat(resolvedWikiLinkService.wikiLinksForViewer(carrier, user), empty());
  }

  @Test
  void outgoing_targets_dedupe_by_resolved_note_id_across_display_text_variants() {
    User user = makeMe.aUser().please();
    Note shared = makeMe.aNote().title("Same").notebookOwnedBy(user).please();
    Note carrier =
        makeMe.aNote().underSameNotebookAs(shared).content("[[Same|a]] [[Same|b]]").please();

    resolvedWikiLinkService.refreshForNote(carrier, user);

    List<Note> outgoing =
        resolvedWikiLinkService.outgoingWikiLinkTargetNotesForViewer(carrier, user);
    assertThat(outgoing, hasSize(1));
    assertThat(outgoing.get(0).getTitle(), equalTo("Same"));
  }
}
