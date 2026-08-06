package com.odde.doughnut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FocusContextRetrievalInboundSamplingTest extends FocusContextRetrievalTestBase {

  private List<String> inboundReferrerTitles(Note focus, User viewer, RetrievalConfig cfg) {
    return service.retrieve(focus, viewer, cfg).getRelatedNotes().stream()
        .filter(n -> n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
        .map(FocusContextNote::getTitle)
        .toList();
  }

  private void addInboundReferrers(Note focus, User viewer, int count, String titlePrefix) {
    for (int i = 0; i < count; i++) {
      Note r =
          makeMe
              .aNote()
              .underSameNotebookAs(focus)
              .title(titlePrefix + i)
              .content("Links to [[" + focus.getTitle() + "]].")
              .please();
      refreshWikiCache(r, viewer);
    }
  }

  @Nested
  class Depth1CapAndSeed {
    private Note focusNote;
    private User viewer;

    @BeforeEach
    void setup() {
      viewer = makeMe.aUser().please();
      focusNote = makeMe.aNote().notebookOwnedBy(viewer).title("HubFocus").please();
      addInboundReferrers(focusNote, viewer, 21, "Ref");
    }

    @Test
    void inboundSampleIsCappedAtSixAndStableForSameSeed() {
      assertInboundSampleStableAndCapped(null);
      assertInboundSampleStableAndCapped(1L);
    }

    @Test
    void differentSeedsCanChangeCappedInboundSet() {
      List<String> seed1Inbound = sortedInboundReferrerTitles(1L);
      boolean foundDistinct =
          LongStream.rangeClosed(2, 5)
              .anyMatch(seed -> !seed1Inbound.equals(sortedInboundReferrerTitles(seed)));
      assertThat(
          "CRC32(concat(noteId, seed)) can rank the same six referrers for two arbitrary seeds; "
              + "expect some seed in range to change the capped set",
          foundDistinct,
          is(true));
    }

    @Test
    void focusNoteInboundReferencesUriListUsesFullCapOfTwenty() {
      assertThat(
          service
              .retrieve(focusNote, viewer, RetrievalConfig.forQuestionGeneration(1L))
              .getFocusNote()
              .getInboundReferences(),
          hasSize(20));
    }

    private List<String> sortedInboundReferrerTitles(long seed) {
      return inboundReferrerTitles(focusNote, viewer, RetrievalConfig.forQuestionGeneration(seed))
          .stream()
          .sorted()
          .toList();
    }

    private void assertInboundSampleStableAndCapped(Long seed) {
      RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(seed);
      List<String> first = inboundReferrerTitles(focusNote, viewer, cfg);
      List<String> second = inboundReferrerTitles(focusNote, viewer, cfg);
      assertThat(first.size(), equalTo(6));
      assertThat(first, equalTo(second));
    }
  }

  @Nested
  class OutgoingExclusionBeforeCap {
    @Test
    void depth1InboundExcludesOutgoingTargetsBeforeCap() {
      User viewer = makeMe.aUser().please();
      Note hub = makeMe.aNote().notebookOwnedBy(viewer).title("XHub").please();
      Note shared =
          makeMe
              .aNote()
              .underSameNotebookAs(hub)
              .title("XShared")
              .content("Links to [[XHub]].")
              .please();
      hub.setContent("[[XShared]].");
      makeMe.entityPersister.merge(hub);
      addInboundReferrers(hub, viewer, 7, "XRef");
      refreshWikiCache(hub, viewer);
      refreshWikiCache(shared, viewer);

      FocusContextResult result =
          service.retrieve(hub, viewer, RetrievalConfig.forQuestionGeneration(null));

      assertThat(
          relatedByTitle(result, "XShared").getEdgeType(),
          equalTo(FocusContextEdgeType.OutgoingWikiLink));
    }
  }

  @Nested
  class Depth2InboundCap {
    @Test
    void depth2InboundCappedAtTwo() {
      User viewer = makeMe.aUser().please();
      Note focusNote = makeMe.aNote().notebookOwnedBy(viewer).title("HubFocus").please();
      Note depth1Ref =
          makeMe
              .aNote()
              .underSameNotebookAs(focusNote)
              .title("Depth1Hub")
              .content("Links to [[HubFocus]].")
              .please();
      refreshWikiCache(depth1Ref, viewer);
      addInboundReferrers(depth1Ref, viewer, 3, "D2Ref");

      FocusContextResult result =
          service.retrieve(focusNote, viewer, RetrievalConfig.forQuestionGeneration(1L));

      long depth2InboundCount =
          result.getRelatedNotes().stream()
              .filter(
                  n ->
                      n.getDepth() == 2
                          && n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
              .count();
      assertThat(depth2InboundCount, lessThanOrEqualTo(2L));
    }
  }
}
