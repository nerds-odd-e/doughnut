package com.odde.doughnut.services;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.graphRAG.*;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import com.odde.doughnut.testability.MakeMe;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
public class GraphRAGServiceTest {
  @Autowired private MakeMe makeMe;
  @Autowired private GraphRAGService graphRAGService;
  @Autowired private NoteService noteService;
  @Autowired private WikiTitleCacheService wikiTitleCacheService;

  // Helper methods for common test operations
  private List<BareNote> getNotesWithRelationship(
      GraphRAGResult result, RelationshipToFocusNote relationship) {
    return result.getRelatedNotes().stream()
        .filter(n -> n.getRelationToFocusNote() == relationship)
        .collect(Collectors.toList());
  }

  private void assertRelatedNotesHaveLinkFromFocus(GraphRAGResult result, Note... expectedNotes) {
    for (Note expected : expectedNotes) {
      BareNote bn =
          result.getRelatedNotes().stream()
              .filter(n -> n.equals(expected))
              .findFirst()
              .orElseThrow(
                  () ->
                      new AssertionError(
                          describeRelatedNotes(result) + " — missing note " + expected.getUri()));
      assertThat(describeRelatedNotes(result), bn.isLinkFromFocus(), is(true));
    }
  }

  private void assertRelatedNotesHaveLinkHop2(GraphRAGResult result, Note... expectedNotes) {
    for (Note expected : expectedNotes) {
      BareNote bn =
          result.getRelatedNotes().stream()
              .filter(n -> n.equals(expected))
              .findFirst()
              .orElseThrow(
                  () ->
                      new AssertionError(
                          describeRelatedNotes(result) + " — missing note " + expected.getUri()));
      assertThat(describeRelatedNotes(result), bn.isLinkHop2(), is(true));
    }
  }

  private void assertRelatedNotesContain(
      GraphRAGResult result, RelationshipToFocusNote relationship, Note... expectedNotes) {
    List<BareNote> notes = getNotesWithRelationship(result, relationship);
    assertThat(describeRelatedNotes(result), notes, hasSize(expectedNotes.length));
    assertThat(notes, containsInAnyOrder((Object[]) expectedNotes));
  }

  private void assertRelatedNotesIncludeNotes(GraphRAGResult result, Note... expectedNotes) {
    List<String> relatedUris = result.getRelatedNotes().stream().map(BareNote::getUri).toList();
    for (Note note : expectedNotes) {
      assertThat(describeRelatedNotes(result), relatedUris, hasItem(note.getUri()));
    }
  }

  private void assertRelatedNotesExcludeNotes(GraphRAGResult result, Note... excludedNotes) {
    List<String> relatedUris = result.getRelatedNotes().stream().map(BareNote::getUri).toList();
    for (Note note : excludedNotes) {
      assertThat(describeRelatedNotes(result), relatedUris, not(hasItem(note.getUri())));
    }
  }

  private String describeRelatedNotes(GraphRAGResult result) {
    return result.getRelatedNotes().stream()
        .map(
            n ->
                (n.getRelationToFocusNote() != null
                        ? String.valueOf(n.getRelationToFocusNote())
                        : "Wiki")
                    + "|ff="
                    + n.isLinkFromFocus()
                    + "|h2="
                    + n.isLinkHop2()
                    + ":"
                    + n.getTitle())
        .collect(Collectors.joining(", "));
  }

  private void refreshWikiCache(Note note) {
    wikiTitleCacheService.refreshForNote(note, note.getCreator());
  }

  @Nested
  class SimpleNoteWithNoParentOrChild {
    @Test
    void zeroBudgetKeepsFullFocusDetailsAndLeavesRelatedNotesEmpty() {
      Note shortNote = makeMe.aNote().title("Short").details("Test Details").please();
      GraphRAGResult shortResult = graphRAGService.retrieve(shortNote, 0, shortNote.getCreator());
      assertThat(shortResult.getFocusNote().getDetails(), equalTo("Test Details"));
      assertThat(shortResult.getRelatedNotes(), empty());

      String longDetails = "a".repeat(2000);
      Note longNote = makeMe.aNote().title("Long").details(longDetails).please();
      GraphRAGResult longResult = graphRAGService.retrieve(longNote, 0, longNote.getCreator());
      assertThat(longResult.getFocusNote().getDetails().length(), equalTo(2000));
      assertThat(longResult.getRelatedNotes(), empty());
    }
  }

  @Nested
  class WhenNoteHasParent {
    private Note parent;
    private Note note;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().title("Parent Note").details("Parent Details").please();
      Notebook notebook = parent.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("parent-child-peers").please();
      parent = makeMe.theNote(parent).folder(peerFolder).please();
      note = makeMe.aNote().under(parent).folder(peerFolder).please();
    }

    @Test
    void folderCrumbOnFocusWithoutLegacyParentExpansionAcrossBudgets() {
      GraphRAGResult full = graphRAGService.retrieve(note, 1000, note.getCreator());
      assertThat(full.getFocusNote().getContextualPath(), containsString("parent-child-peers"));
      assertThat(
          full.getRelatedNotes().stream()
              .noneMatch(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Parent),
          is(true));

      GraphRAGResult zero = graphRAGService.retrieve(note, 0, note.getCreator());
      assertThat(zero.getFocusNote().getContextualPath(), containsString("parent-child-peers"));
      assertThat(zero.getRelatedNotes(), empty());
    }

    @Test
    void truncatesLongDetailWikiTargetInRelatedNotes() {
      String longDetails = "a".repeat(2000);
      Notebook notebook = parent.getNotebook();
      Folder wikiFolder = makeMe.aFolder().notebook(notebook).name("truncate-wiki-peers").please();
      Note wikiTarget =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(note.getCreator())
              .folder(wikiFolder)
              .title("Long Wiki Target")
              .details(longDetails)
              .please();
      makeMe.theNote(note).folder(wikiFolder).details("See [[Long Wiki Target]].").please();
      refreshWikiCache(note);

      GraphRAGResult result = graphRAGService.retrieve(note, 1000, note.getCreator());

      BareNote bn =
          result.getRelatedNotes().stream()
              .filter(b -> b.getUri().equals(wikiTarget.getUri()))
              .findFirst()
              .orElseThrow();
      assertThat(
          bn.getDetails(), equalTo("a".repeat(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "..."));
    }
  }

  @Nested
  class WhenNoteHasTarget {
    private Note target;
    private Note note;
    private Folder peerFolder;

    @BeforeEach
    void setup() {
      Note parentNote = makeMe.aNote().title("Parent Note").please();
      Notebook notebook = parentNote.getNotebook();
      peerFolder = makeMe.aFolder().notebook(notebook).name("target-relation-peers").please();
      parentNote = makeMe.theNote(parentNote).folder(peerFolder).please();
      target =
          makeMe
              .aNote()
              .title("Target Note")
              .details("Target Details")
              .inNotebook(notebook)
              .creator(parentNote.getCreator())
              .folder(peerFolder)
              .please();
      note = makeMe.aRelation().between(parentNote, target).folder(peerFolder).please();
    }

    @Test
    void wikiPrimaryTargetAppearsWhenBudgetAllowsAndSkippedWhenZero() {
      GraphRAGResult full = graphRAGService.retrieve(note, 1000, note.getCreator());
      assertThat(full.getFocusNote().getTitle(), equalTo(note.getTitle()));
      assertThat(
          full.getRelatedNotes().stream().filter(b -> b.equals(target)).count(), equalTo(1L));
      assertRelatedNotesHaveLinkFromFocus(full, target);

      GraphRAGResult zero = graphRAGService.retrieve(note, 0, note.getCreator());
      assertThat(zero.getFocusNote().getTitle(), equalTo(note.getTitle()));
      assertThat(zero.getRelatedNotes(), empty());
    }

    @Test
    void shouldNotDuplicateNoteInRelatedNotesWhenItIsAlsoAChild() {
      // Create a child note that is also the target of the focus note
      makeMe.theNote(target).under(note).please();
      makeMe.refresh(note);

      GraphRAGResult result = graphRAGService.retrieve(note, 1000, note.getCreator());

      assertThat(
          result.getRelatedNotes().stream().filter(b -> b.equals(target)).count(), equalTo(1L));
    }
  }

  @Nested
  class WhenNoteHasSiblingsOfTarget {
    private Note target;
    private Note focusNote;
    private Note targetSibling1;
    private Note targetSibling2;

    @BeforeEach
    void setup() {
      Note parent = makeMe.aNote().title("Parent Note").please();
      Notebook notebook = parent.getNotebook();
      target =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(parent.getCreator())
              .title("Target Note")
              .details("Target Details")
              .please();
      focusNote = makeMe.aRelation().between(parent, target).please();
      refreshWikiCache(focusNote);

      // Create other notes that share the same target
      Note siblingParent1 =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(parent.getCreator())
              .title("Sibling Parent 1")
              .please();
      targetSibling1 = makeMe.aRelation().between(siblingParent1, target).please();
      refreshWikiCache(targetSibling1);

      Note siblingParent2 =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(parent.getCreator())
              .title("Sibling Parent 2")
              .please();
      targetSibling2 = makeMe.aRelation().between(siblingParent2, target).please();
      refreshWikiCache(targetSibling2);
    }

    @Test
    void sharedTargetSiblingsAppearWithWikiHopAndExcludeFocusCarrier() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertRelatedNotesIncludeNotes(result, targetSibling1, targetSibling2);
      assertRelatedNotesHaveLinkHop2(result, targetSibling1, targetSibling2);

      List<BareNote> siblingBare =
          result.getRelatedNotes().stream()
              .filter(n -> n.equals(targetSibling1) || n.equals(targetSibling2))
              .toList();
      assertThat(siblingBare, hasSize(2));
      assertThat(
          siblingBare.stream().map(BareNote::getUri).toList(), not(hasItem(focusNote.getUri())));
    }

    @Test
    void sharedTargetSiblingsOmittedWithZeroBudget() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 0, focusNote.getCreator());
      assertRelatedNotesExcludeNotes(result, targetSibling1, targetSibling2);
    }

    @Test
    void siblingCarrierSubjectsRespectBudget() {
      GraphRAGResult full = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());
      assertRelatedNotesIncludeNotes(full, targetSibling1.getParent(), targetSibling2.getParent());

      GraphRAGResult limited = graphRAGService.retrieve(focusNote, 2, focusNote.getCreator());
      assertRelatedNotesExcludeNotes(
          limited, targetSibling1.getParent(), targetSibling2.getParent());
    }
  }

  @Nested
  class WhenNoteHasYoungerSiblings {
    private Note focusNote;
    private Note youngerSibling1;
    private Note youngerSibling2;

    @BeforeEach
    void setup() {
      Note parent = makeMe.aNote().title("Parent Note").please();
      Notebook notebook = parent.getNotebook();
      Folder peerFolder =
          makeMe.aFolder().notebook(notebook).name("younger-sibling-peers").please();
      focusNote = makeMe.aNote().under(parent).folder(peerFolder).title("Focus Note").please();
      youngerSibling1 =
          makeMe
              .aNote()
              .under(parent)
              .folder(peerFolder)
              .title("Younger One")
              .details("Sibling 1 Details")
              .please();
      youngerSibling2 =
          makeMe
              .aNote()
              .under(parent)
              .folder(peerFolder)
              .title("Younger Two")
              .details("Sibling 2 Details")
              .please();
    }

    @Test
    void youngerSiblingsOnFocusAndRelatedNotesAlign() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertThat(
          result.getFocusNote().getYoungerSiblings(),
          contains(youngerSibling1.getUri(), youngerSibling2.getUri()));
      assertRelatedNotesContain(
          result, RelationshipToFocusNote.YoungerSibling, youngerSibling1, youngerSibling2);
    }
  }

  @Nested
  class WhenNoteHasContextualPath {
    private Note grandParent;
    private Note parent;
    private Note focusNote;

    @BeforeEach
    void setup() {
      grandParent = makeMe.aNote().title("Grand Parent").details("GP Details").please();
      Notebook nb = grandParent.getNotebook();
      Folder outer = makeMe.aFolder().notebook(nb).name("CtxOuter").please();
      Folder inner = makeMe.aFolder().notebook(nb).parentFolder(outer).name("CtxInner").please();
      grandParent = makeMe.theNote(grandParent).folder(inner).please();
      parent = makeMe.aNote().under(grandParent).folder(inner).title("Parent").please();
      focusNote = makeMe.aNote().under(parent).folder(inner).title("Focus").please();
    }

    @Test
    void folderCrumbOnFocusWithoutLegacyContextAncestorRelatedNotes() {
      GraphRAGResult zero = graphRAGService.retrieve(focusNote, 0, focusNote.getCreator());
      assertThat(zero.getFocusNote().getContextualPath(), equalTo("CtxOuter / CtxInner"));

      GraphRAGResult full = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());
      assertThat(getNotesWithRelationship(full, RelationshipToFocusNote.ContextAncestor), empty());
    }
  }

  @Nested
  class WhenNoteHasOlderSiblings {
    private Note olderSibling1;
    private Note olderSibling2;
    private Note focusNote;

    @BeforeEach
    void setup() {
      Note parent = makeMe.aNote().title("Parent Note").please();
      Notebook notebook = parent.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("older-sibling-peers").please();
      olderSibling1 =
          makeMe
              .aNote()
              .under(parent)
              .folder(peerFolder)
              .title("Prior One")
              .details("Sibling 1 Details")
              .please();
      olderSibling2 =
          makeMe
              .aNote()
              .under(parent)
              .folder(peerFolder)
              .title("Prior Two")
              .details("Sibling 2 Details")
              .please();
      focusNote = makeMe.aNote().under(parent).folder(peerFolder).title("Focus Note").please();
    }

    @Test
    void olderSiblingsOnFocusAndRelatedNotesAlign() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertThat(
          result.getFocusNote().getOlderSiblings(),
          contains(olderSibling1.getUri(), olderSibling2.getUri()));

      List<BareNote> siblingNotes =
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.OlderSibling)
              .toList();
      assertThat(siblingNotes, hasSize(2));
      assertThat(
          siblingNotes.stream().map(BareNote::getUriAndTitle).collect(Collectors.toList()),
          containsInAnyOrder(olderSibling1, olderSibling2));
    }
  }

  @Nested
  class WhenNoteHasRelatedChildTarget {
    private Note focusNote;
    private Note relatedChild;
    private Note targetNote;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().title("Focus Note").please();
      Notebook notebook = focusNote.getNotebook();

      // Create the target note first
      targetNote =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(focusNote.getCreator())
              .title("Target Note")
              .details("Target Details")
              .please();

      // Create a relationship between parent and target
      relatedChild = makeMe.aRelation().between(focusNote, targetNote).please();
      refreshWikiCache(relatedChild);
      makeMe.refresh(relatedChild);
    }

    @Test
    void targetAppearsAsWikiLinkNotAsStructuralChild() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertRelatedNotesHaveLinkFromFocus(result, targetNote);
    }
  }

  @Nested
  class WhenNoteHasReferenceByNotes {
    private Note focusNote;
    private Note inboundReferenceParent1;
    private Note inboundReferenceNote1;
    private Note inboundReferenceParent2;
    private Note inboundReferenceNote2;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().title("Focus Note").details("Focus Details").please();
      Notebook notebook = focusNote.getNotebook();

      // Create first inbound reference note
      inboundReferenceParent1 =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(focusNote.getCreator())
              .title("Inbound Reference Parent 1")
              .please();
      inboundReferenceNote1 =
          makeMe.aRelation().between(inboundReferenceParent1, focusNote).please();
      refreshWikiCache(inboundReferenceNote1);

      // Create second inbound reference note
      inboundReferenceParent2 =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(focusNote.getCreator())
              .title("Inbound Reference Parent 2")
              .please();
      inboundReferenceNote2 =
          makeMe.aRelation().between(inboundReferenceParent2, focusNote).please();
      refreshWikiCache(inboundReferenceNote2);
    }

    @Test
    void inboundRefsSubjectsIncludedWhenBudgetEnoughDroppedWhenLimited() {
      GraphRAGResult full = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertThat(
          full.getFocusNote().getInboundReferences(),
          containsInAnyOrder(inboundReferenceNote1.getUri(), inboundReferenceNote2.getUri()));
      assertRelatedNotesHaveLinkFromFocus(full, inboundReferenceNote1, inboundReferenceNote2);
      assertRelatedNotesIncludeNotes(full, inboundReferenceParent1, inboundReferenceParent2);

      GraphRAGResult limited = graphRAGService.retrieve(focusNote, 3, focusNote.getCreator());

      assertThat(limited.getRelatedNotes(), hasSize(2));
      assertRelatedNotesHaveLinkFromFocus(limited, inboundReferenceNote1, inboundReferenceNote2);
      assertRelatedNotesExcludeNotes(limited, inboundReferenceParent1, inboundReferenceParent2);
    }
  }

  @Nested
  class WhenNoteHasCacheOnlyReference {
    @Test
    void shouldIncludeReferenceFromWikiTitleCacheWhenNoLegacyRelationshipFieldExists() {
      Note root = makeMe.aNote().title("Root").please();
      Note focus = makeMe.aNote().under(root).title("Cache Focus").please();
      Note referrerParent = makeMe.aNote().under(root).title("Referrer Parent").please();
      Note referrer =
          makeMe
              .aNote()
              .under(referrerParent)
              .title("Plain Referrer")
              .details("Mentions [[Cache Focus]].")
              .please();
      refreshWikiCache(referrer);

      GraphRAGResult result = graphRAGService.retrieve(focus, 1000, root.getCreator());

      assertThat(result.getFocusNote().getInboundReferences(), contains(referrer.getUri()));
      assertRelatedNotesHaveLinkFromFocus(result, referrer);
    }
  }

  @Nested
  class WhenNoteHasParentSiblings {
    private Note parentSibling1;
    private Note parentSibling2;
    private Note focusNote;
    private Folder peerFolder;

    @BeforeEach
    void setup() {
      Note grandParent = makeMe.aNote().title("Grand Parent").please();
      Notebook notebook = grandParent.getNotebook();
      peerFolder = makeMe.aFolder().notebook(notebook).name("parent-sibling-peers").please();
      grandParent = makeMe.theNote(grandParent).folder(peerFolder).please();
      Note parent = makeMe.aNote().under(grandParent).folder(peerFolder).title("Parent").please();
      parentSibling1 =
          makeMe.aNote().under(grandParent).folder(peerFolder).title("Parent Sibling 1").please();
      parentSibling2 =
          makeMe.aNote().under(grandParent).folder(peerFolder).title("Parent Sibling 2").please();
      focusNote = makeMe.aNote().under(parent).folder(peerFolder).title("Focus Note").please();
    }

    @Test
    void parentSiblingsReachRelatedNotesWhenBudgetAllowsNotAsLegacyParentWhenTight() {
      GraphRAGResult full = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());
      assertRelatedNotesIncludeNotes(full, parentSibling1, parentSibling2);

      GraphRAGResult limited = graphRAGService.retrieve(focusNote, 2, focusNote.getCreator());
      assertThat(
          limited.getRelatedNotes().stream()
              .noneMatch(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Parent),
          is(true));
    }

    @Nested
    class WhenParentSiblingsHaveChildren {
      private Note parentSibling1Child1;
      private Note parentSibling1Child2;
      private Note parentSibling2Child1;

      @BeforeEach
      void setup() {
        parentSibling1Child1 = makeMe.aNote().under(parentSibling1).title("PS1 Child 1").please();
        parentSibling1Child2 = makeMe.aNote().under(parentSibling1).title("PS1 Child 2").please();
        parentSibling2Child1 = makeMe.aNote().under(parentSibling2).title("PS2 Child 1").please();
      }

      @Test
      void parentSiblingChildrenAppearWhenBudgetAllowsDroppedWhenLimited() {
        GraphRAGResult full = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());
        assertRelatedNotesIncludeNotes(
            full, parentSibling1Child1, parentSibling1Child2, parentSibling2Child1);

        GraphRAGResult limited = graphRAGService.retrieve(focusNote, 2, focusNote.getCreator());
        assertRelatedNotesExcludeNotes(
            limited, parentSibling1Child1, parentSibling1Child2, parentSibling2Child1);
      }
    }
  }

  @Nested
  class WhenStructuralPeersUseFolderNotLegacyParentChildren {
    @Test
    void youngerAndOlderSiblingsIgnoreNoteThatSharesParentButNotFolder() {
      Note parent = makeMe.aNote().title("Legacy parent").please();
      Notebook notebook = parent.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("graph-folder-peers").please();
      Note folderOlder =
          makeMe.aNote().under(parent).folder(peerFolder).title("Folder older").please();
      Note focus = makeMe.aNote().under(parent).folder(peerFolder).title("Focus").please();
      Note folderYounger =
          makeMe.aNote().under(parent).folder(peerFolder).title("Folder younger").please();
      makeMe.aNote().under(parent).title("Tree only not in folder").please();

      GraphRAGResult result = graphRAGService.retrieve(focus, 1000, focus.getCreator());

      assertThat(result.getFocusNote().getOlderSiblings(), contains(folderOlder.getUri()));
      assertThat(result.getFocusNote().getYoungerSiblings(), contains(folderYounger.getUri()));
      assertRelatedNotesContain(result, RelationshipToFocusNote.YoungerSibling, folderYounger);
      assertRelatedNotesContain(result, RelationshipToFocusNote.OlderSibling, folderOlder);
    }
  }

  @Nested
  class WhenStructuralPeersAreNotebookRootScope {
    @Test
    void youngerAndOlderSiblingsMatchNotebookRootFolderScopedOrdering() {
      Note a = makeMe.aNote().title("Root A").please();
      Notebook notebook = a.getNotebook();
      var creator = a.getCreator();
      Note b = makeMe.aNote().title("Root B").inNotebook(notebook).creator(creator).please();
      Note c = makeMe.aNote().title("Root C").inNotebook(notebook).creator(creator).please();

      List<Note> peersB = noteService.findStructuralPeerNotesInOrder(b);
      int idx = peersB.indexOf(b);
      List<String> expectedYounger =
          peersB.subList(idx + 1, peersB.size()).stream().map(Note::getUri).toList();
      GraphRAGResult resultMiddle = graphRAGService.retrieve(b, 1000, b.getCreator());
      assertThat(resultMiddle.getFocusNote().getYoungerSiblings(), equalTo(expectedYounger));

      List<Note> peersC = noteService.findStructuralPeerNotesInOrder(c);
      int cIdx = peersC.indexOf(c);
      List<String> expectedOlder = peersC.subList(0, cIdx).stream().map(Note::getUri).toList();
      GraphRAGResult resultLast = graphRAGService.retrieve(c, 1000, c.getCreator());
      assertThat(resultLast.getFocusNote().getOlderSiblings(), equalTo(expectedOlder));
    }
  }

  /**
   * Wiki-link graph contract on serialized GraphRAG: folder crumb string on the focus note, {@code
   * links} plus {@code inboundReferences}, folder peers in related notes, without treating the
   * legacy note parent as graph expansion.
   */
  @Nested
  class WikiLinkGraphContract {

    private List<String> jsonTextArrayElements(JsonNode arrayNode) {
      List<String> out = new ArrayList<>();
      if (arrayNode != null && arrayNode.isArray()) {
        arrayNode.forEach(n -> out.add(n.asText()));
      }
      return out;
    }

    @Test
    void serializedFocusNote_exposesLinksInboundRefsAndFolderCrumbString() {
      Note treeParent = makeMe.aNote().title("713 Tree Parent").please();
      Notebook notebook = treeParent.getNotebook();
      User viewer = treeParent.getCreator();
      Folder outer = makeMe.aFolder().notebook(notebook).name("OuterCrumb").please();
      Folder inner =
          makeMe.aFolder().notebook(notebook).parentFolder(outer).name("InnerCrumb").please();
      treeParent = makeMe.theNote(treeParent).folder(inner).please();

      Note outgoing = makeMe.aNote().under(treeParent).folder(inner).title("713 Outgoing").please();

      Note focus =
          makeMe
              .aNote()
              .under(treeParent)
              .folder(inner)
              .title("713 Focus")
              .details("See [[713 Outgoing]].")
              .please();

      Note referrer =
          makeMe
              .aNote()
              .under(treeParent)
              .folder(inner)
              .title("713 Referrer")
              .details("Ref [[713 Focus]].")
              .please();

      refreshWikiCache(focus);
      refreshWikiCache(referrer);

      GraphRAGResult result = graphRAGService.retrieve(focus, 2500, viewer);
      var mapper = new ObjectMapperConfig().objectMapper();
      JsonNode treeJson = mapper.valueToTree(result);
      JsonNode focusJson = treeJson.get("focusNote");

      assertThat(treeJson.get("relatedNotes").isArray(), is(true));
      assertThat(treeJson.get("relatedNotes").size(), greaterThanOrEqualTo(1));
      assertThat(treeJson.get("relatedNotes").get(0).has("relationToFocusNote"), is(true));

      // Folder-name crumbs from folder ancestry (not legacy note URIs).
      assertThat(focusJson.get("contextualPath").isTextual(), is(true));

      String crumbs = focusJson.get("contextualPath").asText();
      assertThat(crumbs, allOf(containsString("OuterCrumb"), containsString("InnerCrumb")));

      assertThat(focusJson.get("links").isMissingNode(), is(false));
      assertThat(focusJson.get("links").isArray(), is(true));
      assertThat(focusJson.get("links").size(), greaterThanOrEqualTo(1));
      assertThat(focusJson.get("links").toString(), containsString(outgoing.getUri()));

      assertThat(focusJson.get("inboundReferences").isArray(), is(true));
      assertThat(
          jsonTextArrayElements(focusJson.get("inboundReferences")), hasItem(referrer.getUri()));
    }

    @Test
    void relatedNotes_includeFolderPeersAndExcludeLegacyParentExpansion() {
      Note treeParent = makeMe.aNote().title("713 Peer Tree Parent").please();
      Folder inner =
          makeMe.aFolder().notebook(treeParent.getNotebook()).name("713-peer-folder").please();
      treeParent = makeMe.theNote(treeParent).folder(inner).please();

      makeMe.aNote().under(treeParent).folder(inner).title("713 Peer Out Wiki").please();

      Note focus =
          makeMe
              .aNote()
              .under(treeParent)
              .folder(inner)
              .title("713 Peer Focus")
              .details("[[713 Peer Out Wiki]].")
              .please();
      Note younger =
          makeMe.aNote().under(treeParent).folder(inner).title("713 Peer Younger").please();

      refreshWikiCache(focus);

      GraphRAGResult result = graphRAGService.retrieve(focus, 3500, focus.getCreator());

      assertRelatedNotesIncludeNotes(result, younger);
      assertRelatedNotesExcludeNotes(result, treeParent);
    }
  }

  @Nested
  class WhenTargetOfRelationshipHasReferenceBy {
    private Note focusNote;
    private Note relatedChild;
    private Note targetNote;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().title("Focus Note").please();
      Notebook notebook = focusNote.getNotebook();

      targetNote =
          makeMe
              .aNote()
              .inNotebook(notebook)
              .creator(focusNote.getCreator())
              .title("Target Note")
              .details("Target Details")
              .please();

      relatedChild = makeMe.aRelation().between(focusNote, targetNote).please();
      refreshWikiCache(relatedChild);

      makeMe.refresh(targetNote);
    }

    @Test
    void shouldNotIncludeReferencedTargetWhenTargetNotReachedViaStructuralChildWalk() {
      Note referenceParent1 =
          makeMe
              .aNote()
              .inNotebook(focusNote.getNotebook())
              .creator(focusNote.getCreator())
              .title("Reference Parent 1")
              .please();
      refreshWikiCache(makeMe.aRelation().between(referenceParent1, targetNote).please());
      Note referenceParent2 =
          makeMe
              .aNote()
              .inNotebook(focusNote.getNotebook())
              .creator(focusNote.getCreator())
              .title("Reference Parent 2")
              .please();
      refreshWikiCache(makeMe.aRelation().between(referenceParent2, targetNote).please());
      makeMe.refresh(targetNote);

      GraphRAGResult resultHighBudget =
          graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());
      GraphRAGResult resultLowBudget =
          graphRAGService.retrieve(focusNote, 3, focusNote.getCreator());

      for (GraphRAGResult result : List.of(resultHighBudget, resultLowBudget)) {
        for (Note refParent : List.of(referenceParent1, referenceParent2)) {
          result.getRelatedNotes().stream()
              .filter(b -> b.equals(refParent))
              .findFirst()
              .ifPresent(
                  b ->
                      assertThat(
                          describeRelatedNotes(result),
                          b.getRelationToFocusNote(),
                          anyOf(
                              equalTo(RelationshipToFocusNote.RelationshipOfTargetSibling),
                              equalTo(RelationshipToFocusNote.YoungerSibling),
                              equalTo(RelationshipToFocusNote.OlderSibling))));
        }
      }
    }
  }

  @Nested
  class TruncateDetailsTests {
    @Test
    void shouldTruncateASCIICharactersCorrectly() {
      String longDetails = "a".repeat(2000);
      Note parentSeed = makeMe.aNote().title("Truncate ASCII Parent").details(longDetails).please();
      Notebook notebook = parentSeed.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("truncate-peers").please();
      Note parent = makeMe.theNote(parentSeed).folder(peerFolder).please();
      final String parentUri = parent.getUri();
      Note child =
          makeMe
              .aNote()
              .under(parent)
              .folder(peerFolder)
              .details("See [[Truncate ASCII Parent]].")
              .please();
      refreshWikiCache(child);

      GraphRAGResult result = graphRAGService.retrieve(child, 1000, child.getCreator());

      BareNote bn =
          result.getRelatedNotes().stream()
              .filter(b -> b.getUri().equals(parentUri))
              .findFirst()
              .orElseThrow();
      assertThat(
          bn.getDetails(), equalTo("a".repeat(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "..."));
    }

    @Test
    void shouldTruncateCJKCharactersCorrectly() {
      String cjkText = "你好世界".repeat(500);
      Note parentSeed = makeMe.aNote().title("Truncate CJK Parent").details(cjkText).please();
      Notebook notebook = parentSeed.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("truncate-cjk-peers").please();
      Note parent = makeMe.theNote(parentSeed).folder(peerFolder).please();
      final String parentUri = parent.getUri();
      Note child =
          makeMe
              .aNote()
              .under(parent)
              .folder(peerFolder)
              .details("See [[Truncate CJK Parent]].")
              .please();
      refreshWikiCache(child);

      GraphRAGResult result = graphRAGService.retrieve(child, 1000, child.getCreator());

      BareNote bn =
          result.getRelatedNotes().stream()
              .filter(b -> b.getUri().equals(parentUri))
              .findFirst()
              .orElseThrow();
      String truncated = bn.getDetails();

      assertThat(truncated, endsWith("..."));

      byte[] truncatedBytes =
          truncated.substring(0, truncated.length() - 3).getBytes(StandardCharsets.UTF_8);
      assertThat(truncatedBytes.length, lessThanOrEqualTo(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH));

      String withoutEllipsis = truncated.substring(0, truncated.length() - 3);
      assertThat(
          withoutEllipsis.chars().filter(ch -> ch >= 0x4E00 && ch <= 0x9FFF).count() * 3
              + withoutEllipsis.chars().filter(ch -> ch < 0x80).count(),
          lessThanOrEqualTo((long) RELATED_NOTE_DETAILS_TRUNCATE_LENGTH));
    }
  }

  @Nested
  class GetGraphRAGDescriptionTests {
    @Test
    void shouldNotContainNewlinesInJson() {
      Note note = makeMe.aNote().title("Test Note").details("Test Details").please();
      Note child = makeMe.aNote().under(note).title("Child Note").please();

      String description = graphRAGService.getGraphRAGDescription(child);

      // Extract the JSON part (from first { to last })
      int jsonStart = description.indexOf("{");
      int jsonEnd = description.lastIndexOf("}") + 1;
      String jsonPart = description.substring(jsonStart, jsonEnd);
      // Verify no newlines in the JSON content itself
      assertThat(jsonPart, not(containsString("\n")));
      assertThat(jsonPart, not(containsString("\r")));
      // Verify it's valid JSON by parsing it
      try {
        JsonNode jsonNode = new ObjectMapperConfig().objectMapper().readTree(jsonPart);
        assertThat(jsonNode.has("focusNote"), is(true));
      } catch (Exception e) {
        throw new RuntimeException("Generated JSON is invalid", e);
      }
    }
  }
}
