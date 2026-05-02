package com.odde.doughnut.services;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.graphRAG.*;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import com.odde.doughnut.testability.MakeMe;
import java.nio.charset.StandardCharsets;
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
  @Autowired private NoteRepository noteRepository;

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
    void shouldRetrieveJustTheFocusNoteWithZeroBudget() {
      Note note = makeMe.aNote().title("Test Note").details("Test Details").please();

      GraphRAGResult result = graphRAGService.retrieve(note, 0, note.getCreator());

      assertThat(result.getFocusNote().getDetails(), equalTo("Test Details"));
      assertThat(result.getRelatedNotes(), empty());
    }

    @Test
    void shouldNotTruncateFocusNoteDetailsEvenIfItIsVeryLong() {
      String longDetails = "a".repeat(2000);
      Note note = makeMe.aNote().title("Test Note").details(longDetails).please();

      GraphRAGResult result = graphRAGService.retrieve(note, 0, note.getCreator());

      assertThat(result.getFocusNote().getDetails().length(), equalTo(2000));
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
    void shouldIncludeParentInFocusNoteAndContextualPath() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000, note.getCreator());

      assertThat(result.getFocusNote().getContextualPath(), hasItem(parent.getUri()));
    }

    @Test
    void shouldIncludeParentInRelatedNotesWhenBudgetAllows() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000, note.getCreator());

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getRelationToFocusNote(),
          equalTo(RelationshipToFocusNote.Parent));
    }

    @Test
    void shouldNotIncludeParentInRelatedNotesWhenBudgetIsTooSmall() {
      GraphRAGResult result = graphRAGService.retrieve(note, 0, note.getCreator());

      assertThat(result.getFocusNote().getContextualPath(), hasItem(parent.getUri()));
      assertThat(result.getRelatedNotes(), empty());
    }

    @Test
    void shouldTruncateParentDetailsInRelatedNotes() {
      String longDetails = "a".repeat(2000);
      parent.setDetails(longDetails);

      GraphRAGResult result = graphRAGService.retrieve(note, 1000, note.getCreator());

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getDetails(),
          equalTo("a".repeat(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "..."));
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
    void shouldIncludeTargetInRelatedNotesWithFocusTitle() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000, note.getCreator());

      assertThat(result.getFocusNote().getTitle(), equalTo(note.getTitle()));
      assertThat(result.getRelatedNotes(), hasSize(2));
      assertRelatedNotesHaveLinkFromFocus(result, target);
    }

    @Test
    void shouldOmitRelationshipTargetFromRelatedNotesWhenBudgetOnlyAllowsParent() {
      GraphRAGResult result =
          graphRAGService.retrieve(note, 2, note.getCreator()); // Only enough for parent

      assertThat(result.getFocusNote().getTitle(), equalTo(note.getTitle()));

      // Only parent should be in related notes
      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getRelationToFocusNote(),
          equalTo(RelationshipToFocusNote.Parent));
    }

    @Test
    void shouldNotDuplicateNoteInRelatedNotesWhenItIsAlsoAChild() {
      // Create a child note that is also the target of the focus note
      makeMe.theNote(target).under(note).please();
      makeMe.refresh(note);

      GraphRAGResult result = graphRAGService.retrieve(note, 1000, note.getCreator());

      assertThat(result.getFocusNote().getChildren(), contains(target.getUri()));

      // Parent plus target once (relationship carrier excluded from structural child edges)
      assertThat(result.getRelatedNotes(), hasSize(2));
    }

    @Nested
    class WhenTargetHasContextualPath {
      private Note targetGrandParent;
      private Note targetParent;

      @BeforeEach
      void setup() {
        targetGrandParent =
            makeMe
                .aNote()
                .inNotebook(note.getNotebook())
                .creator(note.getCreator())
                .title("Target Grand Parent")
                .folder(peerFolder)
                .please();
        targetParent =
            makeMe
                .aNote()
                .under(targetGrandParent)
                .folder(peerFolder)
                .title("Target Parent")
                .please();
        makeMe.theNote(target).under(targetParent).folder(peerFolder).please();
        makeMe.refresh(target);
      }

      @Test
      void shouldIncludeTargetContextualPathInRelatedNotes() {
        GraphRAGResult result = graphRAGService.retrieve(note, 1000, note.getCreator());

        assertRelatedNotesIncludeNotes(result, targetGrandParent, targetParent);
      }

      @Test
      void shouldNotIncludeTargetContextualPathWhenBudgetIsLimited() {
        // Set budget to only allow target
        GraphRAGResult result = graphRAGService.retrieve(note, 3, note.getCreator());

        // Verify target is included but not its contextual path
        assertThat(result.getRelatedNotes(), hasSize(2));
        assertThat(
            result.getRelatedNotes().stream()
                .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Parent)
                .count(),
            equalTo(1L));
        assertThat(
            result.getRelatedNotes().stream()
                .filter(n -> n.equals(target) && n.isLinkFromFocus())
                .count(),
            equalTo(1L));
        assertThat(
            getNotesWithRelationship(result, RelationshipToFocusNote.TargetContextAncestor),
            empty());
      }
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
    void shouldIncludeSiblingsOfTargetInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertRelatedNotesIncludeNotes(result, targetSibling1, targetSibling2);
      assertRelatedNotesHaveLinkHop2(result, targetSibling1, targetSibling2);
    }

    @Test
    void shouldNotIncludeFocusNoteAsSiblingOfTarget() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      List<BareNote> targetSiblings =
          result.getRelatedNotes().stream()
              .filter(n -> n.equals(targetSibling1) || n.equals(targetSibling2))
              .collect(Collectors.toList());
      assertThat(targetSiblings, hasSize(2));
      assertThat(
          targetSiblings.stream().map(BareNote::getUri).collect(Collectors.toList()),
          not(hasItem(focusNote.getUri())));
    }

    @Test
    void shouldNotIncludeSiblingsOfTargetWhenBudgetIsLimited() {
      // Set budget to only allow parent and target
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 3, focusNote.getCreator());

      // Verify no target siblings are included
      assertRelatedNotesExcludeNotes(result, targetSibling1, targetSibling2);
    }

    @Nested
    class WhenSiblingsOfTargetHaveSubjects {
      @Test
      void shouldIncludeSubjectsOfSiblingsOfTargetInRelatedNotes() {
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

        // Get the parent notes of target siblings
        Note siblingParent1 = targetSibling1.getParent();
        Note siblingParent2 = targetSibling2.getParent();

        assertRelatedNotesIncludeNotes(result, siblingParent1, siblingParent2);
      }

      @Test
      void shouldNotIncludeSubjectsOfSiblingsOfTargetWhenBudgetIsLimited() {
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 2, focusNote.getCreator());

        // Verify no subjects of target siblings are included
        assertRelatedNotesExcludeNotes(
            result, targetSibling1.getParent(), targetSibling2.getParent());
      }
    }
  }

  @Nested
  class WhenNoteHasChildren {
    private Note parent;
    private Note child1;
    private Note child2;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().title("Parent Note").please();
      child1 = makeMe.aNote().under(parent).title("Child One").details("Child 1 Details").please();
      child2 = makeMe.aNote().under(parent).title("Child Two").details("Child 2 Details").please();
    }

    @Test
    void shouldIncludeChildrenInFocusNoteList() {
      GraphRAGResult result = graphRAGService.retrieve(parent, 1000, parent.getCreator());

      assertThat(
          result.getFocusNote().getChildren(),
          containsInAnyOrder(child1.getUri(), child2.getUri()));
    }

    @Test
    void shouldIncludeChildrenInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(parent, 1000, parent.getCreator());

      List<BareNote> childNotes =
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Child)
              .collect(Collectors.toList());

      assertThat(childNotes, hasSize(2));
      assertThat(childNotes, containsInAnyOrder(child1, child2));
    }

    @Test
    void shouldOnlyIncludeChildrenThatFitInBudget() {
      // Set budget to only allow one child
      GraphRAGResult result = graphRAGService.retrieve(parent, 2, parent.getCreator());

      // Only child1 should be in focus note's children list
      assertThat(result.getFocusNote().getChildren(), contains(child1.getUri()));

      // Only child1 should be in related notes
      List<BareNote> childNotes =
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Child)
              .collect(Collectors.toList());

      assertThat(childNotes, hasSize(1));
      assertThat(childNotes.get(0), equalTo(child1));
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
    void shouldIncludeYoungerSiblingsInFocusNoteList() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertThat(
          result.getFocusNote().getYoungerSiblings(),
          contains(youngerSibling1.getUri(), youngerSibling2.getUri()));
    }

    @Test
    void shouldIncludeYoungerSiblingsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertRelatedNotesContain(
          result, RelationshipToFocusNote.YoungerSibling, youngerSibling1, youngerSibling2);
    }

    @Nested
    class AndAlsoHasChildren {
      private Note child1;

      @BeforeEach
      void setup() {
        child1 =
            makeMe.aNote().under(focusNote).title("Child One").details("Child 1 Details").please();
        makeMe.aNote().under(focusNote).title("Child Two").details("Child 2 Details").please();
      }

      @Test
      void shouldAlternateBetweenChildrenAndYoungerSiblingsWhenBudgetIsLimited() {
        // Set budget to only allow two notes
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 4, focusNote.getCreator());

        // Verify in related notes
        List<BareNote> relatedNotes = result.getRelatedNotes();
        assertThat(relatedNotes, hasSize(3));

        // Should have one child and one younger sibling
        assertThat(result.getFocusNote().getChildren(), containsInAnyOrder(child1.getUri()));
        assertThat(result.getFocusNote().getYoungerSiblings(), contains(youngerSibling1.getUri()));

        assertThat(
            relatedNotes.stream()
                .map(BareNote::getRelationToFocusNote)
                .collect(Collectors.toList()),
            containsInAnyOrder(
                RelationshipToFocusNote.Parent,
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.YoungerSibling));
      }
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
      parent = makeMe.aNote().under(grandParent).title("Parent").details("Parent Details").please();
      focusNote = makeMe.aNote().under(parent).title("Focus").please();
    }

    @Test
    void shouldIncludeAncestorsInContextualPathInOrder() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 0, focusNote.getCreator());

      // Should be in contextual path in order from root to parent
      assertThat(
          result.getFocusNote().getContextualPath(),
          contains(grandParent.getUri(), parent.getUri()));
    }

    @Test
    void shouldIncludeNonParentAncestorsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      List<BareNote> contextualNotes =
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.ContextAncestor)
              .collect(Collectors.toList());

      assertThat(
          contextualNotes, hasSize(1)); // Only grandparent, parent is already added as Parent
      assertThat(contextualNotes.get(0), equalTo(grandParent));
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
    void shouldIncludeOlderSiblingsInFocusNoteListInOrder() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertThat(
          result.getFocusNote().getOlderSiblings(),
          contains(olderSibling1.getUri(), olderSibling2.getUri()));
    }

    @Test
    void shouldIncludeOlderSiblingsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      List<BareNote> siblingNotes =
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.OlderSibling)
              .collect(Collectors.toList());

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
    void shouldExcludeRelationChildAndTargetFromStructuralChildren() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertRelatedNotesHaveLinkFromFocus(result, targetNote);
      assertThat(result.getFocusNote().getChildren(), not(contains(relatedChild.getUri())));
      assertThat(
          result.getRelatedNotes().stream()
              .filter(n -> n.getUri().equals(relatedChild.getUri()))
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Child)
              .collect(Collectors.toList()),
          empty());
    }

    @Nested
    class WhenHasMultipleRegularChildren {
      private Note regularChild1;
      private Note regularChild2;
      private Note regularChild3;

      @BeforeEach
      void setup() {
        // Add three regular children
        regularChild1 = makeMe.aNote().under(focusNote).title("Regular Child 1").please();
        regularChild2 = makeMe.aNote().under(focusNote).title("Regular Child 2").please();
        regularChild3 = makeMe.aNote().under(focusNote).title("Regular Child 3").please();

        makeMe.refresh(focusNote);
      }

      @Test
      void shouldKeepRelationChildOutOfStructuralChildrenWhenBudgetIsLimited() {
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 8, focusNote.getCreator());

        assertThat(result.getFocusNote().getChildren(), not(contains(relatedChild.getUri())));
        assertRelatedNotesHaveLinkFromFocus(result, targetNote);
        assertThat(
            result.getRelatedNotes().stream()
                .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Child)
                .count(),
            greaterThanOrEqualTo(1L));
      }

      @Test
      void shouldIncludeAllChildrenWhenBudgetIsEnough() {
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

        assertThat(
            result.getFocusNote().getChildren(),
            containsInAnyOrder(
                regularChild1.getUri(), regularChild2.getUri(), regularChild3.getUri()));
      }

      @Test
      void shouldNotIncludeRelatedChildObjectWhenItComesAfterRegularChildrenAndBudgetIsLimited() {
        makeMe.theNote(relatedChild).after(regularChild3);
        makeMe.refresh(focusNote);

        GraphRAGResult result = graphRAGService.retrieve(focusNote, 8, focusNote.getCreator());

        assertThat(
            result.getFocusNote().getChildren(),
            containsInAnyOrder(
                regularChild1.getUri(), regularChild2.getUri(), regularChild3.getUri()));
        assertThat(
            result.getRelatedNotes().stream()
                .filter(n -> n.getUri().equals(relatedChild.getUri()))
                .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Child)
                .collect(Collectors.toList()),
            empty());
      }
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
    void shouldIncludeReferenceByNotesAndTheirSubjectsWhenBudgetIsEnough() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      // Verify inbound reference notes are in focus note's list
      assertThat(
          result.getFocusNote().getInboundReferences(),
          containsInAnyOrder(inboundReferenceNote1.getUri(), inboundReferenceNote2.getUri()));

      // Verify inbound reference notes are in related notes
      assertRelatedNotesHaveLinkFromFocus(result, inboundReferenceNote1, inboundReferenceNote2);

      // Verify inbound reference subjects are in related notes
      assertRelatedNotesIncludeNotes(result, inboundReferenceParent1, inboundReferenceParent2);
    }

    @Test
    void shouldNotIncludeReferencingNoteSubjectsWhenBudgetIsLimited() {
      // Set budget to only allow inbound reference notes
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 3, focusNote.getCreator());

      // Verify inbound reference relation carriers are included with direct wiki-tier flags only
      assertThat(result.getRelatedNotes(), hasSize(2));
      assertRelatedNotesHaveLinkFromFocus(result, inboundReferenceNote1, inboundReferenceNote2);

      // Verify no inbound reference subjects are included
      assertRelatedNotesExcludeNotes(result, inboundReferenceParent1, inboundReferenceParent2);
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
    void shouldIncludeParentSiblingsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

      assertRelatedNotesIncludeNotes(result, parentSibling1, parentSibling2);
    }

    @Test
    void shouldNotIncludeParentSiblingsWhenBudgetIsLimited() {
      // Set budget to only allow parent
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 2, focusNote.getCreator());

      // Verify only parent is included
      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getRelationToFocusNote(),
          equalTo(RelationshipToFocusNote.Parent));

      assertThat(getNotesWithRelationship(result, RelationshipToFocusNote.ParentSibling), empty());
      assertThat(getNotesWithRelationship(result, RelationshipToFocusNote.OlderSibling), empty());
      assertThat(getNotesWithRelationship(result, RelationshipToFocusNote.YoungerSibling), empty());
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
      void shouldIncludeParentSiblingChildrenInRelatedNotes() {
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000, focusNote.getCreator());

        assertRelatedNotesIncludeNotes(
            result, parentSibling1Child1, parentSibling1Child2, parentSibling2Child1);
      }

      @Test
      void shouldNotIncludeParentSiblingChildrenWhenBudgetIsLimited() {
        // Set budget to only allow parent, parent siblings, and contextual path
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 5, focusNote.getCreator());

        assertThat(describeRelatedNotes(result), result.getRelatedNotes(), hasSize(4));
        assertRelatedNotesExcludeNotes(
            result, parentSibling1Child1, parentSibling1Child2, parentSibling2Child1);
        assertThat(
            getNotesWithRelationship(result, RelationshipToFocusNote.ParentSiblingChild), empty());
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

      List<BareNote> youngerFromGraph =
          getNotesWithRelationship(result, RelationshipToFocusNote.YoungerSibling);
      assertThat(youngerFromGraph, hasSize(1));
      assertThat(
          youngerFromGraph.stream().map(BareNote::getUri).collect(Collectors.toList()),
          contains(folderYounger.getUri()));
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

  @Nested
  class JsonSerialization {
    @Test
    void shouldSerializeRelatedNotesArray() throws Exception {
      Note note = makeMe.aNote().title("Test Note").please();
      Notebook notebook = note.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("json-serial-peers").please();
      note = makeMe.theNote(note).folder(peerFolder).please();
      makeMe.aNote().under(note).folder(peerFolder).title("Child Note").please();

      GraphRAGResult result = graphRAGService.retrieve(note, 1000, note.getCreator());

      JsonNode jsonNode = new ObjectMapperConfig().objectMapper().valueToTree(result);

      assertThat(jsonNode.get("relatedNotes").isArray(), is(true));
      assertThat(jsonNode.get("relatedNotes").size(), equalTo(1));
      assertThat(jsonNode.get("relatedNotes").get(0).has("relationToFocusNote"), is(true));
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
                          equalTo(RelationshipToFocusNote.RelationshipOfTargetSibling)));
        }
      }
    }
  }

  @Nested
  class TruncateDetailsTests {
    @Test
    void shouldTruncateASCIICharactersCorrectly() {
      String longDetails = "a".repeat(2000);
      Note note = makeMe.aNote().title("Test Note").details(longDetails).please();
      Notebook notebook = note.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("truncate-peers").please();
      note = makeMe.theNote(note).folder(peerFolder).please();
      Note child = makeMe.aNote().under(note).folder(peerFolder).please();

      GraphRAGResult result = graphRAGService.retrieve(child, 1000, child.getCreator());

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getDetails(),
          equalTo("a".repeat(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "..."));
    }

    @Test
    void shouldTruncateCJKCharactersCorrectly() {
      // Each CJK character takes 3 bytes in UTF-8
      String cjkText = "你好世界".repeat(500); // 2000 bytes (500 * 4 chars * 3 bytes)
      Note note = makeMe.aNote().title("Test Note").details(cjkText).please();
      Notebook notebook = note.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("truncate-cjk-peers").please();
      note = makeMe.theNote(note).folder(peerFolder).please();
      Note child = makeMe.aNote().under(note).folder(peerFolder).please();

      GraphRAGResult result = graphRAGService.retrieve(child, 1000, child.getCreator());

      assertThat(result.getRelatedNotes(), hasSize(1));

      String truncated = result.getRelatedNotes().get(0).getDetails();

      // Verify it ends with "..."
      assertThat(truncated, endsWith("..."));

      // Verify byte length is within limit
      byte[] truncatedBytes =
          truncated.substring(0, truncated.length() - 3).getBytes(StandardCharsets.UTF_8);
      assertThat(truncatedBytes.length, lessThanOrEqualTo(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH));

      // Verify we didn't cut in the middle of a CJK character
      String withoutEllipsis = truncated.substring(0, truncated.length() - 3);
      assertThat(
          withoutEllipsis.chars().filter(ch -> ch >= 0x4E00 && ch <= 0x9FFF).count() * 3
              + withoutEllipsis.chars().filter(ch -> ch < 0x80).count(),
          lessThanOrEqualTo((long) RELATED_NOTE_DETAILS_TRUNCATE_LENGTH));
    }

    @Test
    void shouldTruncateMixedASCIIAndCJKCorrectly() {
      // Mix of ASCII (1 byte) and CJK (3 bytes)
      String mixedText = "Hello你好World世界".repeat(200);
      Note note = makeMe.aNote().title("Test Note").details(mixedText).please();
      Notebook notebook = note.getNotebook();
      Folder peerFolder = makeMe.aFolder().notebook(notebook).name("truncate-mixed-peers").please();
      note = makeMe.theNote(note).folder(peerFolder).please();
      Note child = makeMe.aNote().under(note).folder(peerFolder).please();

      GraphRAGResult result = graphRAGService.retrieve(child, 1000, child.getCreator());

      assertThat(result.getRelatedNotes(), hasSize(1));
      String truncated = result.getRelatedNotes().get(0).getDetails();
      byte[] truncatedBytes = truncated.getBytes(StandardCharsets.UTF_8);

      // Verify the byte length is correct (excluding "...")
      assertThat(
          truncatedBytes.length, lessThanOrEqualTo(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH + 3));

      // Verify the string ends with "..."
      assertThat(truncated, endsWith("..."));

      // Verify we didn't cut in the middle of a CJK character
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
