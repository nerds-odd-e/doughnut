package com.odde.donut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
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

    FocusContextResult result =
        service.retrieve(
            focus, viewer, new RetrievalConfig(1, null, CONTENT_BUDGET_WITHOUT_FOLDER_PEERS));

    assertThat(relatedTitles(result), hasItem("MidShallow"));
    assertThat(relatedTitles(result), not(hasItem("LeafShallow")));
  }

  @Test
  void cycleBetweenTwoNotesDoesNotLoop() {
    User viewer = makeMe.aUser().please();
    Note a =
        makeMe.aNote().notebookOwnedBy(viewer).title("CycleA").content("To [[CycleB]].").please();
    Note b =
        makeMe.aNote().underSameNotebookAs(a).title("CycleB").content("Back [[CycleA]].").please();

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

    FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

    FocusContextNote directNote = relatedByTitle(result, "DirectShort");
    assertThat(directNote.getDepth(), equalTo(1));
    assertThat(directNote.getRetrievalPath(), hasSize(2));
  }

  @Test
  void depthTwoInboundFromExpandedNote() {
    User viewer = makeMe.aUser().please();
    Note focus = makeMe.aNote().notebookOwnedBy(viewer).title("InboundRoot").please();
    Note hub = makeMe.aNote().underSameNotebookAs(focus).title("HubInbound").please();
    makeMe.authorReferencingContent(hub, "Link [[InboundRoot]].");
    Note depth2Referrer = makeMe.aNote().underSameNotebookAs(focus).title("RefersToHub").please();
    makeMe.authorReferencingContent(depth2Referrer, "Hub is [[HubInbound]].");

    FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

    FocusContextNote hubNote = relatedByTitle(result, "HubInbound");
    assertThat(hubNote.getDepth(), equalTo(1));
    assertThat(isWikiReached(hubNote), is(true));

    FocusContextNote d2 = relatedByTitle(result, "RefersToHub");
    assertThat(d2.getDepth(), equalTo(2));
    assertThat(isWikiReached(d2), is(true));
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

    FocusContextResult result =
        service.retrieve(
            focus,
            viewer,
            new RetrievalConfig(
                2,
                null,
                /* combined content budget: tight enough that depth-1 spends exhaust wiki share before BridgeBudget */
                800));

    List<String> wikiTitles = wikiReachedTitles(result);
    assertThat(wikiTitles, not(hasItem("BridgeBudget")));
    assertThat(wikiTitles, not(hasItem("LeafAfterBudget")));
  }
}
