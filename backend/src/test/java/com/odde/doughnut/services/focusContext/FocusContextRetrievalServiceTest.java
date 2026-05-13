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
              .creator(viewer)
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
      Note longNote =
          makeMe.aNote().creator(viewer).notebook(nb).title("Long").content(longContent).please();
      FocusContextResult longResult = service.retrieve(longNote, viewer, RetrievalConfig.depth1());
      assertThat(longResult.getFocusNote().isContentTruncated(), is(true));
      assertThat(longResult.getFocusNote().getContent().length(), lessThan(longContent.length()));

      Note shortNote =
          makeMe
              .aNote()
              .creator(viewer)
              .notebook(nb)
              .title("Short")
              .content("Small content")
              .please();
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
      focusNote =
          makeMe
              .aNote()
              .creator(viewer)
              .notebook(nb)
              .title("Focus")
              .content("See [[Linked]].")
              .please();
      makeMe
          .aNote()
          .creator(viewer)
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
      focusNote = makeMe.aNote().creator(viewer).notebook(nb).title("Focus").please();
      referrer =
          makeMe
              .aNote()
              .creator(viewer)
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
    private Note focusNote;
    private User viewer;

    @BeforeEach
    void setup() {
      viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      focusNote = makeMe.aNote().creator(viewer).notebook(nb).title("HubFocus").please();
      for (int i = 0; i < 10; i++) {
        Note r =
            makeMe
                .aNote()
                .creator(viewer)
                .underSameNotebookAs(focusNote)
                .title("Ref" + i)
                .content("Links to [[HubFocus]].")
                .please();
        refreshWikiCache(r, viewer);
      }
    }

    @Test
    void depth1InboundCappedAtSix() {
      RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(1L);
      FocusContextResult result = service.retrieve(focusNote, viewer, cfg);

      long inboundCount =
          result.getRelatedNotes().stream()
              .filter(n -> n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
              .count();
      assertThat(inboundCount, equalTo(6L));
    }

    @Test
    void sameSeedProducesSameInboundSelection() {
      RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(1L);
      List<String> first =
          service.retrieve(focusNote, viewer, cfg).getRelatedNotes().stream()
              .filter(n -> n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
              .map(FocusContextNote::getTitle)
              .toList();
      List<String> second =
          service.retrieve(focusNote, viewer, cfg).getRelatedNotes().stream()
              .filter(n -> n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
              .map(FocusContextNote::getTitle)
              .toList();
      assertThat(first, equalTo(second));
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

    @Test
    void noSeedTakesFirstSixInStableOrder() {
      RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(null);
      long count =
          service.retrieve(focusNote, viewer, cfg).getRelatedNotes().stream()
              .filter(n -> n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
              .count();
      assertThat(count, equalTo(6L));
    }

    @Test
    void focusInboundUriListCappedAtTwenty() {
      for (int i = 10; i < 25; i++) {
        Note r =
            makeMe
                .aNote()
                .creator(viewer)
                .underSameNotebookAs(focusNote)
                .title("Extra" + i)
                .content("Links to [[HubFocus]].")
                .please();
        refreshWikiCache(r, viewer);
      }
      RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(1L);
      FocusContextResult result = service.retrieve(focusNote, viewer, cfg);

      assertThat(result.getFocusNote().getInboundReferences(), hasSize(20));
    }

    @Test
    void depth1InboundExcludesOutgoingTargetsBeforeCap() {
      Note hub =
          makeMe.aNote().creator(viewer).underSameNotebookAs(focusNote).title("XHub").please();
      Note shared =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focusNote)
              .title("XShared")
              .content("Links to [[XHub]].")
              .please();
      hub.setContent("[[XShared]].");
      makeMe.entityPersister.merge(hub);
      for (int i = 0; i < 8; i++) {
        makeMe
            .aNote()
            .creator(viewer)
            .underSameNotebookAs(focusNote)
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

    @Test
    void depth2InboundCappedAtTwo() {
      Note depth1Ref =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focusNote)
              .title("Depth1Hub")
              .content("Links to [[HubFocus]].")
              .please();
      refreshWikiCache(depth1Ref, viewer);
      for (int i = 0; i < 5; i++) {
        Note d2 =
            makeMe
                .aNote()
                .creator(viewer)
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

    private List<String> sortedInboundReferrerTitles(long seed) {
      return service
          .retrieve(focusNote, viewer, RetrievalConfig.forQuestionGeneration(seed))
          .getRelatedNotes()
          .stream()
          .filter(n -> n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
          .map(FocusContextNote::getTitle)
          .sorted()
          .toList();
    }
  }

  @Nested
  class Deduplication {
    @Test
    void noteReachedAsBothOutgoingAndInboundKeepsOutgoingEdgeType() {
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focusNote =
          makeMe
              .aNote()
              .creator(viewer)
              .notebook(nb)
              .title("Focus")
              .content("See [[Both]].")
              .please();
      Note both =
          makeMe
              .aNote()
              .creator(viewer)
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
      List<String> titles =
          List.of(
              "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q",
              "R", "S", "T");
      StringBuilder linkLine = new StringBuilder("See ");
      for (String t : titles) {
        linkLine.append("[[").append(t).append("]] ");
      }
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focusNote =
          makeMe
              .aNote()
              .creator(viewer)
              .notebook(nb)
              .title("Focus")
              .content(linkLine.toString().trim() + ".")
              .please();
      String largeDetails = "x".repeat(3500);
      for (String title : titles) {
        makeMe
            .aNote()
            .creator(viewer)
            .underSameNotebookAs(focusNote)
            .title(title)
            .content(largeDetails)
            .please();
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
          makeMe
              .aNote()
              .creator(viewer)
              .notebook(nb)
              .title("ChainRoot")
              .content("Start [[MidDepth]].")
              .please();
      Note mid =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("MidDepth")
              .content("Bridge [[LeafDepth2]].")
              .please();
      makeMe
          .aNote()
          .creator(viewer)
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
          makeMe
              .aNote()
              .creator(viewer)
              .notebook(nb)
              .title("ShallowRoot")
              .content("[[MidShallow]].")
              .please();
      Note mid =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("MidShallow")
              .content("[[LeafShallow]].")
              .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focus)
          .title("LeafShallow")
          .content("deep")
          .please();
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
      Note a =
          makeMe
              .aNote()
              .creator(viewer)
              .notebook(nb)
              .title("CycleA")
              .content("To [[CycleB]].")
              .please();
      Note b =
          makeMe
              .aNote()
              .creator(viewer)
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
              .creator(viewer)
              .notebook(nb)
              .title("ShortFocus")
              .content("[[DirectShort]] [[ViaBridge]].")
              .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focus)
          .title("DirectShort")
          .content("direct body")
          .please();
      Note bridge =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("ViaBridge")
              .content("See [[DirectShort]].")
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
      Note focus = makeMe.aNote().creator(viewer).notebook(nb).title("InboundRoot").please();
      Note hub =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("HubInbound")
              .content("Link [[InboundRoot]].")
              .please();
      Note depth2Referrer =
          makeMe
              .aNote()
              .creator(viewer)
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
      String maxChunk = "z".repeat(2400);
      User viewer = makeMe.aUser().please();
      Notebook nb = notebookReadableBy(viewer);
      Note focus =
          makeMe
              .aNote()
              .creator(viewer)
              .notebook(nb)
              .title("BudgetRoot")
              .content("[[Spend1]] [[Spend2]] [[Spend3]] [[Spend4]] [[Spend5]] [[BridgeBudget]]")
              .please();
      for (int i = 1; i <= 5; i++) {
        makeMe
            .aNote()
            .creator(viewer)
            .underSameNotebookAs(focus)
            .title("Spend" + i)
            .content(maxChunk)
            .please();
      }
      Note bridge =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("BridgeBudget")
              .content("[[LeafAfterBudget]].")
              .please();
      makeMe
          .aNote()
          .creator(viewer)
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
        focus =
            makeMe
                .aNote()
                .creator(viewer)
                .folder(folder)
                .title("FocusSib")
                .content("solo")
                .please();
        for (int i = 0; i < 6; i++) {
          makeMe.aNote().creator(viewer).folder(folder).title("Peer" + i).content("x").please();
        }
      }

      @Test
      void nullSeedRepeatRetrieveSameSiblingOrder() {
        RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(null);
        List<String> first = folderSiblingTitles(service.retrieve(focus, viewer, cfg));
        List<String> second = folderSiblingTitles(service.retrieve(focus, viewer, cfg));

        assertThat(first, equalTo(second));
        assertThat(first.size(), lessThanOrEqualTo(FocusContextConstants.sampleCapAtGraphDepth(1)));
      }

      @Test
      void fixedSeedRepeatRetrieveSameSiblingOrder() {
        RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(42L);
        List<String> a = folderSiblingTitles(service.retrieve(focus, viewer, cfg));
        List<String> b = folderSiblingTitles(service.retrieve(focus, viewer, cfg));

        assertThat(a, equalTo(b));
      }
    }

    @Test
    void largeFolderSampleSiblingsCappedAtSix() {
      User viewer = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(viewer).please();
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
      Notebook nb = makeMe.aNotebook().creatorAndOwner(viewer).please();
      Folder folder = makeMe.aFolder().notebook(nb).please();
      Note focus = makeMe.aNote().folder(folder).please();
      for (int i = 0; i < 15; i++) {
        makeMe.aNote().folder(folder).please();
      }

      List<String> baseline =
          service
              .retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(1L))
              .getFocusNote()
              .getSampleSiblings()
              .stream()
              .sorted()
              .toList();
      boolean foundDistinct =
          LongStream.rangeClosed(2, 10)
              .anyMatch(
                  seed ->
                      !baseline.equals(
                          service
                              .retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(seed))
                              .getFocusNote()
                              .getSampleSiblings()
                              .stream()
                              .sorted()
                              .toList()));

      assertThat(
          "CRC32(concat(noteId, seed)) can yield the same sibling sample for two arbitrary seeds",
          foundDistinct,
          is(true));
    }

    @Test
    void largeNotebookRootSampleSiblingsCappedAtSix() {
      User viewer = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(viewer).please();
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
      Notebook nb = makeMe.aNotebook().creatorAndOwner(viewer).please();
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
      Notebook nb = makeMe.aNotebook().creatorAndOwner(viewer).please();
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
      Notebook nb = makeMe.aNotebook().creatorAndOwner(viewer).please();
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
