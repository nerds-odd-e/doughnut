package com.odde.doughnut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FocusContextRetrievalFolderPeerTest extends FocusContextRetrievalTestBase {

  @Nested
  class SamplingStability {
    private Note focus;
    private User viewer;

    @BeforeEach
    void setup() {
      viewer = makeMe.aUser().please();
      Folder folder = makeMe.aFolder().notebookOwnedBy(viewer).please();
      focus = makeMe.aNote().folder(folder).title("FocusSib").content("solo").please();
      for (int i = 0; i < 6; i++) {
        makeMe.aNote().folder(folder).title("Peer" + i).content("x").please();
      }
    }

    @Test
    void repeatRetrieveSamePeerOrderForNullAndFixedSeed() {
      for (Long seed : new Long[] {null, 42L}) {
        RetrievalConfig cfg = RetrievalConfig.forQuestionGeneration(seed);
        List<String> first = folderPeerTitles(service.retrieve(focus, viewer, cfg));
        List<String> second = folderPeerTitles(service.retrieve(focus, viewer, cfg));
        assertThat(first, equalTo(second));
      }
    }

    @Test
    void peerSampleSizeRespectsDepthOneCap() {
      assertThat(
          folderPeerTitles(
                  service.retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(null)))
              .size(),
          lessThanOrEqualTo(FocusContextConstants.sampleCapAtGraphDepth(1)));
    }
  }

  @Test
  void largeFolderSampleSiblingsCappedAtSix() {
    User viewer = makeMe.aUser().please();
    Folder folder = makeMe.aFolder().notebookOwnedBy(viewer).please();
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
    Folder folder = makeMe.aFolder().notebookOwnedBy(viewer).please();
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
    Note focus = makeMe.aNote().notebookOwnedBy(viewer).please();
    for (int i = 0; i < 22; i++) {
      makeMe.aNote().underSameNotebookAs(focus).please();
    }

    FocusContextResult result =
        service.retrieve(focus, viewer, RetrievalConfig.forQuestionGeneration(null));

    assertThat(result.getFocusNote().getSampleSiblings(), hasSize(6));
  }

  @Test
  void graphApiWithTightBudgetOmitsFolderPeersAndSampleSiblings() {
    User viewer = makeMe.aUser().please();
    Folder folder = makeMe.aFolder().notebookOwnedBy(viewer).please();
    Note focus = makeMe.aNote().folder(folder).please();
    makeMe.aNote().folder(folder).please();

    FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.forGraphApi(10));

    assertThat(result.getFocusNote().getSampleSiblings(), is(empty()));
    assertThat(folderPeerTitles(result), is(empty()));
  }

  @Test
  void folderPeersIncludeStructuralPeersInSameFolder() {
    User viewer = makeMe.aUser().please();
    Folder folder = makeMe.aFolder().notebookOwnedBy(viewer).please();
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

    List<String> peerTitles = folderPeerTitles(result);
    assertThat(peerTitles, hasItem("OtherFolderPeer"));
    assertThat(
        "wiki-resolved targets are not duplicated as folder peers",
        peerTitles,
        not(hasItem("LinkT")));
    assertThat(isWikiReached(relatedByTitle(result, "LinkT")), is(true));
  }

  @Test
  void folderPeerIsNotWikiExpansionFrontier() {
    User viewer = makeMe.aUser().please();
    Note focus =
        makeMe.aNote().notebookOwnedBy(viewer).title("RootFS").content("[[MidFS]].").please();
    Notebook nb = focus.getNotebook();
    Folder folderB = makeMe.aFolder().notebook(nb).please();
    Note mid = makeMe.aNote().folder(folderB).title("MidFS").content("no link to deep").please();
    Note sideSib =
        makeMe.aNote().folder(folderB).title("SideSib").content("[[DeepOnly]].").please();
    Folder folderDeep = makeMe.aFolder().notebook(nb).please();
    makeMe.aNote().folder(folderDeep).title("DeepOnly").content("deep body").please();
    refreshWikiCache(focus, viewer);
    refreshWikiCache(mid, viewer);
    refreshWikiCache(sideSib, viewer);

    FocusContextResult result = service.retrieve(focus, viewer, RetrievalConfig.defaultMaxDepth());

    assertThat(relatedTitles(result), hasItem("MidFS"));
    assertThat(wikiReachedTitles(result), not(hasItem("DeepOnly")));
  }
}
