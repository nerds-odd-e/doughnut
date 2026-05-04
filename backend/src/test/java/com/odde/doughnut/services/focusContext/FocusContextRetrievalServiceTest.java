package com.odde.doughnut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FocusContextRetrievalServiceTest {

  @Autowired private MakeMe makeMe;
  @Autowired private FocusContextRetrievalService service;
  @Autowired private WikiTitleCacheService wikiTitleCacheService;

  private void refreshWikiCache(Note note) {
    wikiTitleCacheService.refreshForNote(note, note.getCreator());
  }

  @Nested
  class FocusNoteOnly {
    @Test
    void noLinksProducesEmptyRelatedNotes() {
      Note note = makeMe.aNote().title("Solo").details("Some content").please();
      FocusContextResult result =
          service.retrieve(note, note.getCreator(), RetrievalConfig.depth1());

      assertThat(result.getFocusNote().getTitle(), equalTo("Solo"));
      assertThat(result.getRelatedNotes(), empty());
    }

    @Test
    void focusNoteDetailsTruncatedWhenExceedsMaxTokens() {
      String longDetails = "a".repeat(10000);
      Note note = makeMe.aNote().title("Long").details(longDetails).please();
      FocusContextResult result =
          service.retrieve(note, note.getCreator(), RetrievalConfig.depth1());

      assertThat(result.getFocusNote().isDetailsTruncated(), is(true));
      assertThat(result.getFocusNote().getDetails().length(), lessThan(longDetails.length()));
    }

    @Test
    void focusNoteDetailsNotTruncatedWhenWithinBudget() {
      Note note = makeMe.aNote().title("Short").details("Small content").please();
      FocusContextResult result =
          service.retrieve(note, note.getCreator(), RetrievalConfig.depth1());

      assertThat(result.getFocusNote().isDetailsTruncated(), is(false));
      assertThat(result.getFocusNote().getDetails(), equalTo("Small content"));
    }
  }

  @Nested
  class OutgoingWikiLinks {
    private Note focusNote;
    private Note linkedNote;
    private User viewer;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().title("Focus").details("See [[Linked]].").please();
      viewer = focusNote.getCreator();
      linkedNote =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focusNote)
              .title("Linked")
              .details("Linked content")
              .please();
      refreshWikiCache(focusNote);
    }

    @Test
    void outgoingWikiLinkTargetEmittedWithCorrectEdgeType() {
      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getRelatedNotes(), hasSize(1));
      FocusContextNote related = result.getRelatedNotes().get(0);
      assertThat(related.getTitle(), equalTo("Linked"));
      assertThat(related.getEdgeType(), equalTo(FocusContextEdgeType.OutgoingWikiLink));
      assertThat(related.getDepth(), equalTo(1));
    }

    @Test
    void outgoingWikiLinkRetrievalPathContainsFocusAndTarget() {
      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      FocusContextNote related = result.getRelatedNotes().get(0);
      List<String> path = related.getRetrievalPath();
      assertThat(path, hasSize(2));
      assertThat(path.get(0), equalTo("[[Focus]]"));
      assertThat(path.get(1), containsString("Linked"));
    }
  }

  @Nested
  class InboundWikiReferences {
    private Note focusNote;
    private Note referrer;
    private User viewer;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().title("Focus").please();
      viewer = focusNote.getCreator();
      referrer =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focusNote)
              .title("Referrer")
              .details("Links to [[Focus]].")
              .please();
      refreshWikiCache(referrer);
    }

    @Test
    void inboundReferrerEmittedWithCorrectEdgeType() {
      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getRelatedNotes(), hasSize(1));
      FocusContextNote related = result.getRelatedNotes().get(0);
      assertThat(related.getTitle(), equalTo("Referrer"));
      assertThat(related.getEdgeType(), equalTo(FocusContextEdgeType.InboundWikiReference));
    }
  }

  @Nested
  class Deduplication {
    @Test
    void noteReachedAsBothOutgoingAndInboundKeepsOutgoingEdgeType() {
      Note focusNote = makeMe.aNote().title("Focus").details("See [[Both]].").please();
      User viewer = focusNote.getCreator();
      Note both =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focusNote)
              .title("Both")
              .details("Links back to [[Focus]].")
              .please();
      refreshWikiCache(focusNote);
      refreshWikiCache(both);

      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      List<FocusContextNote> related = result.getRelatedNotes();
      assertThat(related, hasSize(1));
      assertThat(related.get(0).getTitle(), equalTo("Both"));
      assertThat(related.get(0).getEdgeType(), equalTo(FocusContextEdgeType.OutgoingWikiLink));
    }
  }

  @Nested
  class TokenBudget {
    @Test
    void relatedNotesBudgetCapsNumberOfIncludedNotes() {
      Note focusNote =
          makeMe
              .aNote()
              .title("Focus")
              .details("See [[A]] [[B]] [[C]] [[D]] [[E]] [[F]].")
              .please();
      User viewer = focusNote.getCreator();
      String largeDetails = "x".repeat(3500);
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focusNote)
          .title("A")
          .details(largeDetails)
          .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focusNote)
          .title("B")
          .details(largeDetails)
          .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focusNote)
          .title("C")
          .details(largeDetails)
          .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focusNote)
          .title("D")
          .details(largeDetails)
          .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focusNote)
          .title("E")
          .details(largeDetails)
          .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focusNote)
          .title("F")
          .details(largeDetails)
          .please();
      refreshWikiCache(focusNote);

      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getRelatedNotes().size(), lessThan(6));
    }
  }
}
