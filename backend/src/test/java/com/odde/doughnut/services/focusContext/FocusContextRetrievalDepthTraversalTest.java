package com.odde.doughnut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import java.util.List;
import org.junit.jupiter.api.Test;

class FocusContextRetrievalDepthTraversalTest extends FocusContextRetrievalTestBase {

  @Test
  void outgoingChainReachesDepthTwoLeaf() {
    User viewer = makeMe.aUser().please();
    Note focus =
        makeMe
            .aNote()
            .notebookOwnedBy(viewer)
            .title("ChainRoot")
            .content("Start [[MidDepth]].")
            .please();
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

    FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

    assertThat(relatedTitles(result), hasItems("MidDepth", "LeafDepth2"));
    FocusContextNote leafNote = relatedByTitle(result, "LeafDepth2");
    assertThat(leafNote.getDepth(), equalTo(2));
    assertThat(leafNote.getRetrievalPath(), hasSize(3));
    assertThat(leafNote.getRetrievalPath().get(0), equalTo("[[ChainRoot]]"));
    assertThat(leafNote.getRetrievalPath().get(2), containsString("LeafDepth2"));
  }

  @Test
  void maxDepthOneSkipsSecondHop() {
    User viewer = makeMe.aUser().please();
    Note focus =
        makeMe
            .aNote()
            .notebookOwnedBy(viewer)
            .title("ShallowRoot")
            .content("[[MidShallow]].")
            .please();
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

    assertThat(relatedTitles(result), hasItem("MidShallow"));
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
    Note a =
        makeMe.aNote().notebookOwnedBy(viewer).title("CycleA").content("To [[CycleB]].").please();
    Note b =
        makeMe.aNote().underSameNotebookAs(a).title("CycleB").content("Back [[CycleA]].").please();
    refreshWikiCache(a, viewer);
    refreshWikiCache(b, viewer);

    FocusContextResult result = service.retrieve(a, viewer, RetrievalConfig.defaultMaxDepth());

    assertThat(result.getRelatedNotes(), hasSize(1));
    assertThat(result.getRelatedNotes().get(0).getTitle(), equalTo("CycleB"));
  }

  @Test
  void shorterPathWinsWhenSameNoteReachableAtDepthOneAndTwo() {
    User viewer = makeMe.aUser().please();
    Note focus =
        makeMe
            .aNote()
            .notebookOwnedBy(viewer)
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

    FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

    FocusContextNote directNote = relatedByTitle(result, "DirectShort");
    assertThat(directNote.getDepth(), equalTo(1));
    assertThat(directNote.getRetrievalPath(), hasSize(2));
  }

  @Test
  void depthTwoInboundFromExpandedNote() {
    User viewer = makeMe.aUser().please();
    Note focus = makeMe.aNote().notebookOwnedBy(viewer).title("InboundRoot").please();
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

    FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

    FocusContextNote hubNote = relatedByTitle(result, "HubInbound");
    assertThat(hubNote.getDepth(), equalTo(1));
    assertThat(hubNote.getEdgeType(), equalTo(FocusContextEdgeType.InboundWikiReference));

    FocusContextNote d2 = relatedByTitle(result, "RefersToHub");
    assertThat(d2.getDepth(), equalTo(2));
    assertThat(d2.getEdgeType(), equalTo(FocusContextEdgeType.InboundWikiReference));
  }

  @Test
  void budgetExhaustedMidRingLeavesLaterDepthOneNotesAndDepthTwoUnreachable() {
    String heavyBody = "z".repeat(600);
    User viewer = makeMe.aUser().please();
    Note focus =
        makeMe
            .aNote()
            .notebookOwnedBy(viewer)
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
