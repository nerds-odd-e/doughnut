package com.odde.donut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FocusContextRetrievalServiceTest extends FocusContextRetrievalTestBase {

  @Nested
  class FocusNoteOnly {
    @Test
    void noLinksProducesEmptyRelatedNotes() {
      User viewer = makeMe.aUser().please();
      Note note =
          makeMe.aNote().notebookOwnedBy(viewer).title("Solo").content("Some content").please();

      FocusContextResult result = service.retrieve(note, viewer, RetrievalConfig.depth1());

      assertThat(result.getFocusNote().getTitle(), equalTo("Solo"));
      assertThat(result.getRelatedNotes(), empty());
    }

    @Test
    void longFocusContentIsTruncated() {
      User viewer = makeMe.aUser().please();
      String longContent = "a".repeat(10000);
      Note longNote =
          makeMe.aNote().notebookOwnedBy(viewer).title("Long").content(longContent).please();

      FocusContextResult result = service.retrieve(longNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getFocusNote().isContentTruncated(), is(true));
      assertThat(result.getFocusNote().getContent().length(), lessThan(longContent.length()));
    }

    @Test
    void shortFocusContentIsNotTruncated() {
      User viewer = makeMe.aUser().please();
      Note shortNote =
          makeMe.aNote().notebookOwnedBy(viewer).title("Short").content("Small content").please();

      FocusContextResult result = service.retrieve(shortNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getFocusNote().isContentTruncated(), is(false));
      assertThat(result.getFocusNote().getContent(), equalTo("Small content"));
    }
  }

  @Nested
  class OutgoingWikiLinks {
    private Note focusNote;
    private User viewer;

    @BeforeEach
    void setup() {
      viewer = makeMe.aUser().please();
      focusNote =
          makeMe.aNote().notebookOwnedBy(viewer).title("Focus").content("See [[Linked]].").please();
      makeMe
          .aNote()
          .underSameNotebookAs(focusNote)
          .title("Linked")
          .content("Linked content")
          .please();
    }

    @Test
    void outgoingWikiLinkEmitsTargetWithDepthAndPath() {
      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getRelatedNotes(), hasSize(1));
      FocusContextNote related = result.getRelatedNotes().get(0);
      assertThat(related.getTitle(), equalTo("Linked"));
      assertThat(related.getDepth(), equalTo(1));
      List<String> path = related.getRetrievalPath();
      assertThat(path, hasSize(2));
      assertThat(path.get(0), equalTo("[[Focus]]"));
      assertThat(path.get(1), containsString("Linked"));
    }
  }

  @Nested
  class InboundWikiReferences {
    private Note focusNote;
    private User viewer;

    @BeforeEach
    void setup() {
      viewer = makeMe.aUser().please();
      focusNote = makeMe.aNote().notebookOwnedBy(viewer).title("Focus").please();
      Note referrer = makeMe.aNote().underSameNotebookAs(focusNote).title("Referrer").please();
      makeMe.authorReferencingContent(referrer, "Links to [[Focus]].");
    }

    @Test
    void inboundReferrerIsEmitted() {
      // Folder peers disabled: focusNote and referrer are notebook-root structural siblings, so a
      // budget that allows folder-peer sampling would let "Referrer" appear via that unrelated path
      // and mask a regression in inbound wiki-reference discovery.
      FocusContextResult result =
          service.retrieve(
              focusNote, viewer, new RetrievalConfig(1, null, CONTENT_BUDGET_WITHOUT_FOLDER_PEERS));

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(result.getRelatedNotes().get(0).getTitle(), equalTo("Referrer"));
    }
  }

  @Nested
  class Deduplication {
    @Test
    void noteReachedAsBothOutgoingAndInboundAppearsOnce() {
      User viewer = makeMe.aUser().please();
      Note focusNote =
          makeMe.aNote().notebookOwnedBy(viewer).title("Focus").content("See [[Both]].").please();
      Note both = makeMe.aNote().underSameNotebookAs(focusNote).title("Both").please();
      makeMe.authorReferencingContent(both, "Links back to [[Focus]].");

      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(result.getRelatedNotes().get(0).getTitle(), equalTo("Both"));
    }
  }
}
