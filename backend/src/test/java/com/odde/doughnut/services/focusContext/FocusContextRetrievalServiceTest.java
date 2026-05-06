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
      Note note = makeMe.aNote().title("Solo").details("Some content").please();
      FocusContextResult result =
          service.retrieve(note, note.getCreator(), RetrievalConfig.depth1());

      assertThat(result.getFocusNote().getTitle(), equalTo("Solo"));
      assertThat(result.getRelatedNotes(), empty());
    }

    @Test
    void focusNoteDetailsTruncationMatchesApproximateTokenBudget() {
      String longDetails = "a".repeat(10000);
      Note longNote = makeMe.aNote().title("Long").details(longDetails).please();
      FocusContextResult longResult =
          service.retrieve(longNote, longNote.getCreator(), RetrievalConfig.depth1());
      assertThat(longResult.getFocusNote().isDetailsTruncated(), is(true));
      assertThat(longResult.getFocusNote().getDetails().length(), lessThan(longDetails.length()));

      Note shortNote = makeMe.aNote().title("Short").details("Small content").please();
      FocusContextResult shortResult =
          service.retrieve(shortNote, shortNote.getCreator(), RetrievalConfig.depth1());
      assertThat(shortResult.getFocusNote().isDetailsTruncated(), is(false));
      assertThat(shortResult.getFocusNote().getDetails(), equalTo("Small content"));
    }
  }

  @Nested
  class OutgoingWikiLinks {
    private Note focusNote;
    private User viewer;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().title("Focus").details("See [[Linked]].").please();
      viewer = focusNote.getCreator();
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
  class InboundSampling {
    private Note focusNote;
    private User viewer;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().title("HubFocus").please();
      viewer = focusNote.getCreator();
      for (int i = 0; i < 10; i++) {
        Note r =
            makeMe
                .aNote()
                .creator(viewer)
                .underSameNotebookAs(focusNote)
                .title("Ref" + i)
                .details("Links to [[HubFocus]].")
                .please();
        refreshWikiCache(r);
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
      List<String> seed1 =
          service
              .retrieve(focusNote, viewer, RetrievalConfig.forQuestionGeneration(1L))
              .getRelatedNotes()
              .stream()
              .filter(n -> n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
              .map(FocusContextNote::getTitle)
              .sorted()
              .toList();
      List<String> seed2 =
          service
              .retrieve(focusNote, viewer, RetrievalConfig.forQuestionGeneration(99L))
              .getRelatedNotes()
              .stream()
              .filter(n -> n.getEdgeType() == FocusContextEdgeType.InboundWikiReference)
              .map(FocusContextNote::getTitle)
              .sorted()
              .toList();
      assertThat(seed1, not(equalTo(seed2)));
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
                .details("Links to [[HubFocus]].")
                .please();
        refreshWikiCache(r);
      }
      RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(1L);
      FocusContextResult result = service.retrieve(focusNote, viewer, cfg);

      assertThat(result.getFocusNote().getInboundReferences(), hasSize(20));
    }

    @Test
    void depth2InboundCappedAtTwo() {
      Note depth1Ref =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focusNote)
              .title("Depth1Hub")
              .details("Links to [[HubFocus]].")
              .please();
      refreshWikiCache(depth1Ref);
      for (int i = 0; i < 5; i++) {
        Note d2 =
            makeMe
                .aNote()
                .creator(viewer)
                .underSameNotebookAs(focusNote)
                .title("D2Ref" + i)
                .details("Links to [[Depth1Hub]].")
                .please();
        refreshWikiCache(d2);
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
      for (String title : List.of("A", "B", "C", "D", "E", "F")) {
        makeMe
            .aNote()
            .creator(viewer)
            .underSameNotebookAs(focusNote)
            .title(title)
            .details(largeDetails)
            .please();
      }
      refreshWikiCache(focusNote);

      FocusContextResult result = service.retrieve(focusNote, viewer, RetrievalConfig.depth1());

      assertThat(result.getRelatedNotes().size(), lessThan(6));
    }
  }

  @Nested
  class BreadthFirstDepth2 {
    @Test
    void outgoingChainReachesDepthTwoLeaf() {
      Note focus = makeMe.aNote().title("ChainRoot").details("Start [[MidDepth]].").please();
      User viewer = focus.getCreator();
      Note mid =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("MidDepth")
              .details("Bridge [[LeafDepth2]].")
              .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focus)
          .title("LeafDepth2")
          .details("Only at depth 2")
          .please();
      refreshWikiCache(focus);
      refreshWikiCache(mid);

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
      Note focus = makeMe.aNote().title("ShallowRoot").details("[[MidShallow]].").please();
      User viewer = focus.getCreator();
      Note mid =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("MidShallow")
              .details("[[LeafShallow]].")
              .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focus)
          .title("LeafShallow")
          .details("deep")
          .please();
      refreshWikiCache(focus);
      refreshWikiCache(mid);

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
      Note a = makeMe.aNote().title("CycleA").details("To [[CycleB]].").please();
      User viewer = a.getCreator();
      Note b =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(a)
              .title("CycleB")
              .details("Back [[CycleA]].")
              .please();
      refreshWikiCache(a);
      refreshWikiCache(b);

      FocusContextResult result = service.retrieve(a, viewer, RetrievalConfig.defaultMaxDepth());

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(result.getRelatedNotes().get(0).getTitle(), equalTo("CycleB"));
    }

    @Test
    void shorterPathWinsWhenSameNoteReachableAtDepthOneAndTwo() {
      Note focus =
          makeMe.aNote().title("ShortFocus").details("[[DirectShort]] [[ViaBridge]].").please();
      User viewer = focus.getCreator();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focus)
          .title("DirectShort")
          .details("direct body")
          .please();
      Note bridge =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("ViaBridge")
              .details("See [[DirectShort]].")
              .please();
      refreshWikiCache(focus);
      refreshWikiCache(bridge);

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
      Note focus = makeMe.aNote().title("InboundRoot").please();
      User viewer = focus.getCreator();
      Note hub =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("HubInbound")
              .details("Link [[InboundRoot]].")
              .please();
      Note depth2Referrer =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("RefersToHub")
              .details("Hub is [[HubInbound]].")
              .please();
      refreshWikiCache(hub);
      refreshWikiCache(depth2Referrer);

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
      Note focus =
          makeMe
              .aNote()
              .title("BudgetRoot")
              .details("[[Spend1]] [[Spend2]] [[Spend3]] [[Spend4]] [[Spend5]] [[BridgeBudget]]")
              .please();
      User viewer = focus.getCreator();
      for (int i = 1; i <= 5; i++) {
        makeMe
            .aNote()
            .creator(viewer)
            .underSameNotebookAs(focus)
            .title("Spend" + i)
            .details(maxChunk)
            .please();
      }
      Note bridge =
          makeMe
              .aNote()
              .creator(viewer)
              .underSameNotebookAs(focus)
              .title("BridgeBudget")
              .details("[[LeafAfterBudget]].")
              .please();
      makeMe
          .aNote()
          .creator(viewer)
          .underSameNotebookAs(focus)
          .title("LeafAfterBudget")
          .details("never reached")
          .please();
      refreshWikiCache(focus);
      refreshWikiCache(bridge);

      FocusContextResult result =
          service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

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
      private Notebook nb;
      private Folder folder;
      private Note focus;
      private User viewer;

      @BeforeEach
      void setup() {
        nb = makeMe.aNotebook().please();
        folder = makeMe.aFolder().notebook(nb).please();
        focus =
            makeMe.aNote().inNotebook(nb).folder(folder).title("FocusSib").details("solo").please();
        viewer = focus.getCreator();
        for (int i = 0; i < 6; i++) {
          makeMe
              .aNote()
              .creator(viewer)
              .inNotebook(nb)
              .folder(folder)
              .title("Peer" + i)
              .details("x")
              .please();
        }
      }

      @Test
      void nullSeedRepeatRetrieveSameSiblingOrder() {
        RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(null);
        List<String> first = folderSiblingTitles(service.retrieve(focus, viewer, cfg));
        List<String> second = folderSiblingTitles(service.retrieve(focus, viewer, cfg));

        assertThat(first, equalTo(second));
        assertThat(
            first.size(), lessThanOrEqualTo(FocusContextConstants.MAX_FOLDER_SIBLINGS_PER_NOTE));
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
    void graphApiWithTightBudgetOmitsFolderPeersAndSampleSiblings() {
      Notebook nb = makeMe.aNotebook().please();
      Folder folder = makeMe.aFolder().notebook(nb).please();
      User viewer = makeMe.aUser().please();
      Note focus =
          makeMe
              .aNote()
              .creatorAndOwner(viewer)
              .inNotebook(nb)
              .folder(folder)
              .title("TightA")
              .details("solo")
              .please();
      makeMe
          .aNote()
          .creatorAndOwner(viewer)
          .inNotebook(nb)
          .folder(folder)
          .title("TightB")
          .details("solo")
          .please();

      FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.forGraphApi(10));

      assertThat(result.getFocusNote().getSampleSiblings(), is(empty()));
      assertThat(folderSiblingTitles(result), is(empty()));
    }

    @Test
    void folderSiblingsIncludeStructuralPeersInSameFolder() {
      Notebook nb = makeMe.aNotebook().please();
      Folder folder = makeMe.aFolder().notebook(nb).please();
      Note focus =
          makeMe
              .aNote()
              .inNotebook(nb)
              .folder(folder)
              .title("FocusF")
              .details("See [[LinkT]].")
              .please();
      User viewer = focus.getCreator();
      Note linkT =
          makeMe
              .aNote()
              .creator(viewer)
              .inNotebook(nb)
              .folder(folder)
              .title("LinkT")
              .details("target")
              .please();
      makeMe
          .aNote()
          .creator(viewer)
          .inNotebook(nb)
          .folder(folder)
          .title("OtherFolderPeer")
          .details("from same folder as link target")
          .please();
      refreshWikiCache(focus);
      refreshWikiCache(linkT);

      FocusContextResult result =
          service.retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(null));

      List<String> siblingTitles = folderSiblingTitles(result);
      assertThat(siblingTitles, hasItem("LinkT"));
      assertThat(siblingTitles, hasItem("OtherFolderPeer"));
    }

    @Test
    void folderSiblingIsNotWikiExpansionFrontier() {
      Note focus = makeMe.aNote().title("RootFS").details("[[MidFS]].").please();
      User viewer = focus.getCreator();
      Notebook nb = focus.getNotebook();
      Folder folderB = makeMe.aFolder().notebook(nb).please();
      Note mid =
          makeMe
              .aNote()
              .creator(viewer)
              .inNotebook(nb)
              .folder(folderB)
              .title("MidFS")
              .details("no link to deep")
              .please();
      Note sideSib =
          makeMe
              .aNote()
              .creator(viewer)
              .inNotebook(nb)
              .folder(folderB)
              .title("SideSib")
              .details("[[DeepOnly]].")
              .please();
      Folder folderDeep = makeMe.aFolder().notebook(nb).please();
      makeMe
          .aNote()
          .creator(viewer)
          .inNotebook(nb)
          .folder(folderDeep)
          .title("DeepOnly")
          .details("deep body")
          .please();
      refreshWikiCache(focus);
      refreshWikiCache(mid);
      refreshWikiCache(sideSib);

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
