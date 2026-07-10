package com.odde.doughnut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.stream.LongStream;
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

  private void refreshWikiCache(Note note, User viewer) {
    wikiTitleCacheService.refreshForNote(note, viewer);
  }

  private Notebook notebookReadableBy(User viewer) {
    return makeMe.aNotebook().creatorAndOwner(viewer).please();
  }

  private static List<String> folderSiblingTitles(FocusContextResult result) {
    return result.getRelatedNotes().stream()
        .filter(n -> n.getEdgeType() == FocusContextEdgeType.FolderSibling)
        .map(FocusContextNote::getTitle)
        .toList();
  }

  @Nested
  class FocusNoteOnly {
    @Test
    void noLinksProducesEmptyRelatedNotes() {
      User viewer = makeMe.aUser().please();
      Note note =
          makeMe
              .aNote()
              .notebook(notebookReadableBy(viewer))
              .title("Solo")
              .content("Some content")
              .please();
      FocusContextResult result = service.retrieve(note, viewer, RetrievalConfig.depth1());

      assertThat(result.getFocusNote().getTitle(), equalTo("Solo"));
      assertThat(result.getRelatedNotes(), empty());
    }

    @Test
    void focusNoteContentTruncationMatchesApproximateTokenBudget() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      String longContent = "a".repeat(10000);
      Note longNote = makeMe.aNote().notebook(nb).title("Long").content(longContent).please();
      FocusContextResult longResult = service.retrieve(longNote, viewer, RetrievalConfig.depth1());
      assertThat(longResult.getFocusNote().isContentTruncated(), is(true));
      assertThat(longResult.getFocusNote().getContent().length(), lessThan(longContent.length()));

      Note shortNote = makeMe.aNote().notebook(nb).title("Short").content("Small content").please();
      FocusContextResult shortResult =
          service.retrieve(shortNote, viewer, RetrievalConfig.depth1());
      assertThat(shortResult.getFocusNote().isContentTruncated(), is(false));
      assertThat(shortResult.getFocusNote().getContent(), equalTo("Small content"));
    }
  }

  @Nested
  class OutgoingWikiLinks {
    private Note focusNote;
    private User viewer;

    @BeforeEach
    void setup() {
      viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      focusNote = makeMe.aNote().notebook(nb).title("Focus").content("See [[Linked]].").please();
      makeMe
          .aNote()
          .underSameNotebookAs(focusNote)
          .title("Linked")
          .content("Linked content")
          .please();
      refreshWikiCache(focusNote, viewer);
    }

    @Test
    void outgoingWikiLinkEmitsTargetWithEdgeTypeDepthAndPath() {
      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getRelatedNotes(), hasSize(1));
      FocusContextNote related = result.getRelatedNotes().get(0);
      assertThat(related.getTitle(), equalTo("Linked"));
      assertThat(related.getEdgeType(), equalTo(FocusContextEdgeType.OutgoingWikiLink));
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
    private Note referrer;
    private User viewer;

    @BeforeEach
    void setup() {
      viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      focusNote = makeMe.aNote().notebook(nb).title("Focus").please();
      referrer =
          makeMe
              .aNote()
              .underSameNotebookAs(focusNote)
              .title("Referrer")
              .content("Links to [[Focus]].")
              .please();
      refreshWikiCache(referrer, viewer);
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
  class InboundSampling {
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
        Notebook nb = notebookReadableBy(viewer);
        focusNote = makeMe.aNote().notebook(nb).title("HubFocus").please();
        addInboundReferrers(focusNote, viewer, 21, "Ref");
      }

      @Test
      void differentSeedsProduceDifferentInboundSelection() {
        List<String> baseline = sortedInboundReferrerTitles(1L);
        boolean foundDistinct =
            LongStream.rangeClosed(2, 10)
                .anyMatch(seed -> !baseline.equals(sortedInboundReferrerTitles(seed)));
        assertThat(
            "CRC32(concat(noteId, seed)) can rank the same six referrers for two arbitrary seeds; "
                + "expect some seed in range to change the capped set",
            foundDistinct,
            is(true));
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

      @Test
      void inboundSamplingCapStabilityAndUriList() {
        assertInboundSampleStableAndCapped(null);
        assertInboundSampleStableAndCapped(1L);
        assertThat(
            service
                .retrieve(focusNote, viewer, RetrievalConfig.forQuestionGeneration(1L))
                .getFocusNote()
                .getInboundReferences(),
            hasSize(20));
      }
    }

    @Nested
    class OutgoingExclusionBeforeCap {
      @Test
      void depth1InboundExcludesOutgoingTargetsBeforeCap() {
        User viewer = makeMe.aUser().please();
        Notebook nb = notebookReadableBy(viewer);
        Note hub = makeMe.aNote().notebook(nb).title("XHub").please();
        Note shared =
            makeMe
                .aNote()
                .underSameNotebookAs(hub)
                .title("XShared")
                .content("Links to [[XHub]].")
                .please();
        hub.setContent("[[XShared]].");
        makeMe.entityPersister.merge(hub);
        for (int i = 0; i < 7; i++) {
          makeMe
              .aNote()
              .underSameNotebookAs(hub)
              .title("XRef" + i)
              .content("Links to [[XHub]].")
              .please();
        }
        refreshWikiCache(hub, viewer);
        refreshWikiCache(shared, viewer);

        FocusContextResult result =
            service.retrieve(hub, viewer, RetrievalConfig.forQuestionGeneration(null));

        assertThat(
            result.getRelatedNotes().stream()
                .filter(n -> "XShared".equals(n.getTitle()))
                .noneMatch(n -> n.getEdgeType() == FocusContextEdgeType.InboundWikiReference),
            is(true));
        assertThat(
            result.getRelatedNotes().stream()
                .filter(n -> "XShared".equals(n.getTitle()))
                .anyMatch(n -> n.getEdgeType() == FocusContextEdgeType.OutgoingWikiLink),
            is(true));
      }
    }

    @Nested
    class Depth2InboundCap {
      @Test
      void depth2InboundCappedAtTwo() {
        User viewer = makeMe.aUser().please();
        Notebook nb = notebookReadableBy(viewer);
        Note focusNote = makeMe.aNote().notebook(nb).title("HubFocus").please();
        Note depth1Ref =
            makeMe
                .aNote()
                .underSameNotebookAs(focusNote)
                .title("Depth1Hub")
                .content("Links to [[HubFocus]].")
                .please();
        refreshWikiCache(depth1Ref, viewer);
        for (int i = 0; i < 3; i++) {
          Note d2 =
              makeMe
                  .aNote()
                  .underSameNotebookAs(focusNote)
                  .title("D2Ref" + i)
                  .content("Links to [[Depth1Hub]].")
                  .please();
          refreshWikiCache(d2, viewer);
        }

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

  @Nested
  class Deduplication {
    @Test
    void noteReachedAsBothOutgoingAndInboundKeepsOutgoingEdgeType() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focusNote = makeMe.aNote().notebook(nb).title("Focus").content("See [[Both]].").please();
      Note both =
          makeMe
              .aNote()
              .underSameNotebookAs(focusNote)
              .title("Both")
              .content("Links back to [[Focus]].")
              .please();
      refreshWikiCache(focusNote, viewer);
      refreshWikiCache(both, viewer);

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
      List<String> titles = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");
      StringBuilder linkLine = new StringBuilder("See ");
      for (String t : titles) {
        linkLine.append("[[").append(t).append("]] ");
      }
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focusNote =
          makeMe
              .aNote()
              .notebook(nb)
              .title("Focus")
              .content(linkLine.toString().trim() + ".")
              .please();
      String largeDetails = "x".repeat(3000);
      for (String title : titles) {
        makeMe.aNote().underSameNotebookAs(focusNote).title(title).content(largeDetails).please();
      }
      refreshWikiCache(focusNote, viewer);

      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getRelatedNotes().size(), lessThan(titles.size()));
    }
  }

  @Nested
  class BreadthFirstDepth2 {
    @Test
    void outgoingChainReachesDepthTwoLeaf() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focus =
          makeMe.aNote().notebook(nb).title("ChainRoot").content("Start [[MidDepth]].").please();
      Note mid =
          makeMe
              .aNote()
              .underSameNotebookAs(focus)
              .title("MidDepth")
              .content("Bridge [[LeafDepth2]].")
              .please();
      makeMe
          .aNote()
          .underSameNotebookAs(focus)
          .title("LeafDepth2")
          .content("Only at depth 2")
          .please();
      refreshWikiCache(focus, viewer);
      refreshWikiCache(mid, viewer);

      FocusContextResult result =
          service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

      List<String> titles =
          result.getRelatedNotes().stream().map(FocusContextNote::getTitle).toList();
      assertThat(titles, hasItems("MidDepth", "LeafDepth2"));
      FocusContextNote leafNote =
          result.getRelatedNotes().stream()
              .filter(n -> "LeafDepth2".equals(n.getTitle()))
              .findFirst()
              .orElseThrow();
      assertThat(leafNote.getDepth(), equalTo(2));
      assertThat(leafNote.getEdgeType(), equalTo(FocusContextEdgeType.OutgoingWikiLink));
      assertThat(leafNote.getRetrievalPath(), hasSize(3));
      assertThat(leafNote.getRetrievalPath().get(0), equalTo("[[ChainRoot]]"));
      assertThat(leafNote.getRetrievalPath().get(2), containsString("LeafDepth2"));
    }

    @Test
    void maxDepthOneSkipsSecondHop() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focus =
          makeMe.aNote().notebook(nb).title("ShallowRoot").content("[[MidShallow]].").please();
      Note mid =
          makeMe
              .aNote()
              .underSameNotebookAs(focus)
              .title("MidShallow")
              .content("[[LeafShallow]].")
              .please();
      makeMe.aNote().underSameNotebookAs(focus).title("LeafShallow").content("deep").please();
      refreshWikiCache(focus, viewer);
      refreshWikiCache(mid, viewer);

      FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.depth1());

      assertThat(
          result.getRelatedNotes().stream().map(FocusContextNote::getTitle).toList(),
          hasItem("MidShallow"));
      assertThat(
          result.getRelatedNotes().stream()
              .filter(n -> n.getEdgeType() == FocusContextEdgeType.OutgoingWikiLink)
              .map(FocusContextNote::getTitle)
              .toList(),
          not(hasItem("LeafShallow")));
    }

    @Test
    void cycleBetweenTwoNotesDoesNotLoop() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note a = makeMe.aNote().notebook(nb).title("CycleA").content("To [[CycleB]].").please();
      Note b =
          makeMe
              .aNote()
              .underSameNotebookAs(a)
              .title("CycleB")
              .content("Back [[CycleA]].")
              .please();
      refreshWikiCache(a, viewer);
      refreshWikiCache(b, viewer);

      FocusContextResult result = service.retrieve(a, viewer, RetrievalConfig.defaultMaxDepth());

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(result.getRelatedNotes().get(0).getTitle(), equalTo("CycleB"));
    }

    @Test
    void shorterPathWinsWhenSameNoteReachableAtDepthOneAndTwo() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focus =
          makeMe
              .aNote()
              .notebook(nb)
              .title("ShortFocus")
              .content("[[DirectShort]] [[ViaBridge]]")
              .please();
      makeMe.aNote().underSameNotebookAs(focus).title("DirectShort").please();
      Note bridge =
          makeMe
              .aNote()
              .underSameNotebookAs(focus)
              .title("ViaBridge")
              .content("[[DirectShort]]")
              .please();
      refreshWikiCache(focus, viewer);
      refreshWikiCache(bridge, viewer);

      FocusContextResult result =
          service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

      FocusContextNote directNote =
          result.getRelatedNotes().stream()
              .filter(n -> "DirectShort".equals(n.getTitle()))
              .findFirst()
              .orElseThrow();
      assertThat(directNote.getDepth(), equalTo(1));
      assertThat(directNote.getEdgeType(), equalTo(FocusContextEdgeType.OutgoingWikiLink));
      assertThat(directNote.getRetrievalPath(), hasSize(2));
    }

    @Test
    void depthTwoInboundFromExpandedNote() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focus = makeMe.aNote().notebook(nb).title("InboundRoot").please();
      Note hub =
          makeMe
              .aNote()
              .underSameNotebookAs(focus)
              .title("HubInbound")
              .content("Link [[InboundRoot]].")
              .please();
      Note depth2Referrer =
          makeMe
              .aNote()
              .underSameNotebookAs(focus)
              .title("RefersToHub")
              .content("Hub is [[HubInbound]].")
              .please();
      refreshWikiCache(hub, viewer);
      refreshWikiCache(depth2Referrer, viewer);

      FocusContextResult result =
          service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

      FocusContextNote hubNote =
          result.getRelatedNotes().stream()
              .filter(n -> "HubInbound".equals(n.getTitle()))
              .findFirst()
              .orElseThrow();
      assertThat(hubNote.getDepth(), equalTo(1));
      assertThat(hubNote.getEdgeType(), equalTo(FocusContextEdgeType.InboundWikiReference));

      FocusContextNote d2 =
          result.getRelatedNotes().stream()
              .filter(n -> "RefersToHub".equals(n.getTitle()))
              .findFirst()
              .orElseThrow();
      assertThat(d2.getDepth(), equalTo(2));
      assertThat(d2.getEdgeType(), equalTo(FocusContextEdgeType.InboundWikiReference));
    }

    @Test
    void budgetExhaustedMidRingLeavesLaterDepthOneNotesAndDepthTwoUnreachable() {
      String heavyBody = "z".repeat(600);
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focus =
          makeMe
              .aNote()
              .notebook(nb)
              .title("BudgetRoot")
              .content("[[Spend1]] [[Spend2]] [[Spend3]] [[Spend4]] [[BridgeBudget]]")
              .please();
      for (int i = 1; i <= 4; i++) {
        makeMe.aNote().underSameNotebookAs(focus).title("Spend" + i).content(heavyBody).please();
      }
      Note bridge =
          makeMe
              .aNote()
              .underSameNotebookAs(focus)
              .title("BridgeBudget")
              .content("[[LeafAfterBudget]].")
              .please();
      makeMe
          .aNote()
          .underSameNotebookAs(focus)
          .title("LeafAfterBudget")
          .content("never reached")
          .please();
      refreshWikiCache(focus, viewer);
      refreshWikiCache(bridge, viewer);

      FocusContextResult result =
          service.retrieve(
              focus,
              viewer,
              new RetrievalConfig(
                  2,
                  null,
                  /* combined content budget: tight enough that depth-1 spends exhaust wiki share before BridgeBudget */
                  800));

      List<String> wikiTitles =
          result.getRelatedNotes().stream()
              .filter(
                  n ->
                      n.getEdgeType() == FocusContextEdgeType.OutgoingWikiLink
                          || n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
              .map(FocusContextNote::getTitle)
              .toList();
      assertThat(wikiTitles, not(hasItem("BridgeBudget")));
      assertThat(wikiTitles, not(hasItem("LeafAfterBudget")));
    }
  }

  @Nested
  class FolderSiblings {
    @Nested
    class SamplingStability {
      private Note focus;
      private User viewer;

      @BeforeEach
      void setup() {
        viewer = makeMe.aUser().please();
        Notebook nb = notebookReadableBy(viewer);
        Folder folder = makeMe.aFolder().notebook(nb).please();
        focus = makeMe.aNote().folder(folder).title("FocusSib").content("solo").please();
        for (int i = 0; i < 6; i++) {
          makeMe.aNote().folder(folder).title("Peer" + i).content("x").please();
        }
      }

      @Test
      void repeatRetrieveSameSiblingOrderForNullAndFixedSeed() {
        for (Long seed : new Long[] {null, 42L}) {
          RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(seed);
          List<String> first = folderSiblingTitles(service.retrieve(focus, viewer, cfg));
          List<String> second = folderSiblingTitles(service.retrieve(focus, viewer, cfg));
          assertThat(first, equalTo(second));
        }
        assertThat(
            folderSiblingTitles(
                    service.retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(null)))
                .size(),
            lessThanOrEqualTo(FocusContextConstants.sampleCapAtGraphDepth(1)));
      }
    }

    @Test
    void largeFolderSampleSiblingsCappedAtSix() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Folder folder = makeMe.aFolder().notebook(nb).please();
      Note focus = makeMe.aNote().folder(folder).please();
      for (int i = 0; i < 25; i++) {
        makeMe.aNote().folder(folder).please();
      }

      FocusContextResult result =
          service.retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(null));

      assertThat(result.getFocusNote().getSampleSiblings(), hasSize(6));
    }

    @Test
    void differentSeedsProduceDifferentSampleSiblingsSelection() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Folder folder = makeMe.aFolder().notebook(nb).please();
      Note focus = makeMe.aNote().folder(folder).please();
      for (int i = 0; i < 10; i++) {
        makeMe.aNote().folder(folder).please();
      }

      List<String> baseline = sortedSampleSiblingTitles(focus, viewer, 1L);
      boolean foundDistinct =
          LongStream.rangeClosed(2, 10)
              .anyMatch(seed -> !baseline.equals(sortedSampleSiblingTitles(focus, viewer, seed)));

      assertThat(
          "CRC32(concat(noteId, seed)) can yield the same sibling sample for two arbitrary seeds",
          foundDistinct,
          is(true));
    }

    private List<String> sortedSampleSiblingTitles(Note focus, User viewer, long seed) {
      return service
          .retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(seed))
          .getFocusNote()
          .getSampleSiblings()
          .stream()
          .sorted()
          .toList();
    }

    @Test
    void largeNotebookRootSampleSiblingsCappedAtSix() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focus = makeMe.aNote().notebook(nb).please();
      for (int i = 0; i < 22; i++) {
        makeMe.aNote().notebook(nb).please();
      }

      FocusContextResult result =
          service.retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(null));

      assertThat(result.getFocusNote().getSampleSiblings(), hasSize(6));
    }

    @Test
    void graphApiWithTightBudgetOmitsFolderPeersAndSampleSiblings() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Folder folder = makeMe.aFolder().notebook(nb).please();
      Note focus = makeMe.aNote().folder(folder).please();
      makeMe.aNote().folder(folder).please();

      FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.forGraphApi(10));

      assertThat(result.getFocusNote().getSampleSiblings(), is(empty()));
      assertThat(folderSiblingTitles(result), is(empty()));
    }

    @Test
    void folderSiblingsIncludeStructuralPeersInSameFolder() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Folder folder = makeMe.aFolder().notebook(nb).please();
      Note focus = makeMe.aNote().folder(folder).title("FocusF").content("See [[LinkT]].").please();
      Note linkT = makeMe.aNote().folder(folder).title("LinkT").content("target").please();
      makeMe
          .aNote()
          .folder(folder)
          .title("OtherFolderPeer")
          .content("from same folder as link target")
          .please();
      refreshWikiCache(focus, viewer);
      refreshWikiCache(linkT, viewer);

      FocusContextResult result =
          service.retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(null));

      List<String> siblingTitles = folderSiblingTitles(result);
      assertThat(siblingTitles, hasItem("OtherFolderPeer"));
      assertThat(
          "wiki-resolved targets are not duplicated as folder siblings",
          siblingTitles,
          not(hasItem("LinkT")));
      assertThat(
          result.getRelatedNotes().stream()
              .filter(n -> n.getEdgeType() == FocusContextEdgeType.OutgoingWikiLink)
              .map(FocusContextNote::getTitle)
              .toList(),
          hasItem("LinkT"));
    }

    @Test
    void folderSiblingIsNotWikiExpansionFrontier() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focus = makeMe.aNote().title("RootFS").content("[[MidFS]].").notebook(nb).please();
      Folder folderB = makeMe.aFolder().notebook(nb).please();
      Note mid = makeMe.aNote().folder(folderB).title("MidFS").content("no link to deep").please();
      Note sideSib =
          makeMe.aNote().folder(folderB).title("SideSib").content("[[DeepOnly]].").please();
      Folder folderDeep = makeMe.aFolder().notebook(nb).please();
      makeMe.aNote().folder(folderDeep).title("DeepOnly").content("deep body").please();
      refreshWikiCache(focus, viewer);
      refreshWikiCache(mid, viewer);
      refreshWikiCache(sideSib, viewer);

      FocusContextResult result =
          service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

      List<String> titles =
          result.getRelatedNotes().stream().map(FocusContextNote::getTitle).toList();
      assertThat(titles, hasItem("MidFS"));
      assertThat(
          result.getRelatedNotes().stream()
              .filter(n -> "DeepOnly".equals(n.getTitle()))
              .noneMatch(n -> n.getEdgeType() == FocusContextEdgeType.OutgoingWikiLink),
          is(true));
    }
  }
}
