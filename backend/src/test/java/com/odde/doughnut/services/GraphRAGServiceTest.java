package com.odde.doughnut.services;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import com.odde.doughnut.testability.MakeMe;
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

  private final GraphRAGService graphRAGService =
      new GraphRAGService(new OneTokenPerNoteStrategy());

  // Helper methods for common test operations
  private List<BareNote> getNotesWithRelationship(
      GraphRAGResult result, RelationshipToFocusNote relationship) {
    return result.getRelatedNotes().stream()
        .filter(n -> n.getRelationToFocusNote() == relationship)
        .collect(Collectors.toList());
  }

  private String getUriAndTitle(Note note) {
    return "[" + note.getTopicConstructor() + "](/n" + note.getId() + ")";
  }

  private void assertRelatedNotesContain(
      GraphRAGResult result, RelationshipToFocusNote relationship, String... expectedUriAndTitles) {
    List<BareNote> notes = getNotesWithRelationship(result, relationship);
    assertThat(notes, hasSize(expectedUriAndTitles.length));
    assertThat(
        notes.stream().map(BareNote::getUriAndTitle).collect(Collectors.toList()),
        containsInAnyOrder(expectedUriAndTitles));
  }

  @Test
  void shouldRetrieveJustTheFocusNoteWithZeroBudget() {
    Note note = makeMe.aNote().titleConstructor("Test Note").details("Test Details").please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    assertThat(result.getFocusNote().getDetails(), equalTo("Test Details"));
    assertThat(result.getRelatedNotes(), empty());
  }

  @Test
  void shouldNotTruncateFocusNoteDetailsEvenIfItIsVeryLong() {
    String longDetails = "a".repeat(2000);
    Note note = makeMe.aNote().titleConstructor("Test Note").details(longDetails).please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    assertThat(result.getFocusNote().getDetails().length(), equalTo(2000));
  }

  @Nested
  class WhenNoteHasParent {
    private Note parent;
    private Note note;
    private String expectedParentUriAndTitle;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().titleConstructor("Parent Note").details("Parent Details").please();
      note = makeMe.aNote().under(parent).please();
      expectedParentUriAndTitle = "[Parent Note](/n" + parent.getId() + ")";
    }

    @Test
    void shouldIncludeParentInFocusNoteAndContextualPath() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      assertThat(result.getFocusNote().getParentUriAndTitle(), equalTo(expectedParentUriAndTitle));
    }

    @Test
    void shouldIncludeParentInRelatedNotesWhenBudgetAllows() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getRelationToFocusNote(),
          equalTo(RelationshipToFocusNote.Parent));
    }

    @Test
    void shouldNotIncludeParentInRelatedNotesWhenBudgetIsTooSmall() {
      GraphRAGResult result = graphRAGService.retrieve(note, 0);

      assertThat(result.getFocusNote().getParentUriAndTitle(), equalTo(expectedParentUriAndTitle));
      assertThat(result.getRelatedNotes(), empty());
    }

    @Test
    void shouldTruncateParentDetailsInRelatedNotes() {
      String longDetails = "a".repeat(2000);
      parent.setDetails(longDetails);

      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getDetails(),
          equalTo("a".repeat(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "..."));
    }
  }

  @Nested
  class WhenNoteHasObject {
    private Note parent;
    private Note target;
    private Note note;
    private String expectedTargetUriAndTitle;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().titleConstructor("Parent Note").please();
      target = makeMe.aNote().titleConstructor("Target Note").details("Target Details").please();
      note = makeMe.aLink().between(parent, target).please();
      expectedTargetUriAndTitle = "[Target Note](/n" + target.getId() + ")";
    }

    @Test
    void shouldIncludeObjectInFocusNote() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      assertThat(result.getFocusNote().getObjectUriAndTitle(), equalTo(expectedTargetUriAndTitle));
    }

    @Test
    void shouldIncludeObjectInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      assertThat(result.getRelatedNotes(), hasSize(2));
      assertThat(
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Object)
              .findFirst()
              .get()
              .getUriAndTitle(),
          equalTo(expectedTargetUriAndTitle));
    }

    @Test
    void shouldKeepObjectInFocusNoteEvenWhenBudgetOnlyAllowsParent() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1); // Only enough for parent

      // Object URI should still be in focus note
      assertThat(result.getFocusNote().getObjectUriAndTitle(), equalTo(expectedTargetUriAndTitle));

      // Only parent should be in related notes
      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getRelationToFocusNote(),
          equalTo(RelationshipToFocusNote.Parent));
    }

    @Test
    void shouldNotDuplicateNoteInRelatedNotesWhenItIsAlsoAChild() {
      // Create a child note that is also the object of the focus note
      makeMe.theNote(target).under(note).please();
      makeMe.refresh(note);

      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      // Should be in both object and children lists of focus note
      assertThat(
          result.getFocusNote().getObjectUriAndTitle(),
          equalTo("[Target Note](/n" + target.getId() + ")"));
      assertThat(
          result.getFocusNote().getChildren(), contains("[Target Note](/n" + target.getId() + ")"));

      // But should appear only once in related notes
      assertThat(result.getRelatedNotes(), hasSize(2)); // parent and child/object
    }
  }

  @Nested
  class WhenNoteHasChildren {
    private Note parent;
    private Note child1;
    private Note child2;
    private String expectedChild1UriAndTitle;
    private String expectedChild2UriAndTitle;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().titleConstructor("Parent Note").please();
      child1 =
          makeMe
              .aNote()
              .under(parent)
              .titleConstructor("Child One")
              .details("Child 1 Details")
              .please();
      child2 =
          makeMe
              .aNote()
              .under(parent)
              .titleConstructor("Child Two")
              .details("Child 2 Details")
              .please();
      expectedChild1UriAndTitle = "[Child One](/n" + child1.getId() + ")";
      expectedChild2UriAndTitle = "[Child Two](/n" + child2.getId() + ")";
    }

    @Test
    void shouldIncludeChildrenInFocusNoteList() {
      GraphRAGResult result = graphRAGService.retrieve(parent, 1000);

      assertThat(
          result.getFocusNote().getChildren(),
          containsInAnyOrder(expectedChild1UriAndTitle, expectedChild2UriAndTitle));
    }

    @Test
    void shouldIncludeChildrenInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(parent, 1000);

      List<BareNote> childNotes =
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Child)
              .collect(Collectors.toList());

      assertThat(childNotes, hasSize(2));
      assertThat(
          childNotes.stream().map(BareNote::getUriAndTitle).collect(Collectors.toList()),
          containsInAnyOrder(expectedChild1UriAndTitle, expectedChild2UriAndTitle));
    }

    @Test
    void shouldOnlyIncludeChildrenThatFitInBudget() {
      // Set budget to only allow one child
      GraphRAGResult result = graphRAGService.retrieve(parent, 1);

      // Only child1 should be in focus note's children list
      assertThat(result.getFocusNote().getChildren(), contains(expectedChild1UriAndTitle));

      // Only child1 should be in related notes
      List<BareNote> childNotes =
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Child)
              .collect(Collectors.toList());

      assertThat(childNotes, hasSize(1));
      assertThat(childNotes.get(0).getUriAndTitle(), equalTo(expectedChild1UriAndTitle));
    }
  }

  @Nested
  class WhenNoteHasYoungerSiblings {
    private Note parent;
    private Note focusNote;
    private Note youngerSibling1;
    private Note youngerSibling2;
    private String expectedYoungerSibling1UriAndTitle;
    private String expectedYoungerSibling2UriAndTitle;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().titleConstructor("Parent Note").please();
      focusNote = makeMe.aNote().under(parent).titleConstructor("Focus Note").please();
      youngerSibling1 =
          makeMe
              .aNote()
              .under(parent)
              .titleConstructor("Younger One")
              .details("Sibling 1 Details")
              .please();
      youngerSibling2 =
          makeMe
              .aNote()
              .under(parent)
              .titleConstructor("Younger Two")
              .details("Sibling 2 Details")
              .please();
      expectedYoungerSibling1UriAndTitle = "[Younger One](/n" + youngerSibling1.getId() + ")";
      expectedYoungerSibling2UriAndTitle = "[Younger Two](/n" + youngerSibling2.getId() + ")";
    }

    @Test
    void shouldIncludeYoungerSiblingsInFocusNoteList() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      assertThat(
          result.getFocusNote().getYoungerSiblings(),
          contains(expectedYoungerSibling1UriAndTitle, expectedYoungerSibling2UriAndTitle));
    }

    @Test
    void shouldIncludeYoungerSiblingsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      assertRelatedNotesContain(
          result,
          RelationshipToFocusNote.YoungerSibling,
          expectedYoungerSibling1UriAndTitle,
          expectedYoungerSibling2UriAndTitle);
    }

    @Nested
    class AndAlsoHasChildren {
      private Note child1;
      private Note child2;
      private String expectedChild1UriAndTitle;

      @BeforeEach
      void setup() {
        child1 =
            makeMe
                .aNote()
                .under(focusNote)
                .titleConstructor("Child One")
                .details("Child 1 Details")
                .please();
        child2 =
            makeMe
                .aNote()
                .under(focusNote)
                .titleConstructor("Child Two")
                .details("Child 2 Details")
                .please();
        expectedChild1UriAndTitle = "[Child One](/n" + child1.getId() + ")";
      }

      @Test
      void shouldAlternateBetweenChildrenAndYoungerSiblingsWhenBudgetIsLimited() {
        // Set budget to only allow two notes
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 3);

        // Verify in related notes
        List<BareNote> relatedNotes = result.getRelatedNotes();
        assertThat(relatedNotes, hasSize(3));

        // Should have one child and one younger sibling
        assertThat(result.getFocusNote().getChildren(), contains(expectedChild1UriAndTitle));
        assertThat(
            result.getFocusNote().getYoungerSiblings(),
            contains(expectedYoungerSibling1UriAndTitle));

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
    private String expectedGrandParentUriAndTitle;
    private String expectedParentUriAndTitle;

    @BeforeEach
    void setup() {
      grandParent = makeMe.aNote().titleConstructor("Grand Parent").details("GP Details").please();
      parent =
          makeMe
              .aNote()
              .under(grandParent)
              .titleConstructor("Parent")
              .details("Parent Details")
              .please();
      focusNote = makeMe.aNote().under(parent).titleConstructor("Focus").please();
      expectedGrandParentUriAndTitle = "[Grand Parent](/n" + grandParent.getId() + ")";
      expectedParentUriAndTitle = "[Parent](/n" + parent.getId() + ")";
    }

    @Test
    void shouldIncludeAncestorsInContextualPathInOrder() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 0);

      // Should be in contextual path in order from root to parent
      assertThat(
          result.getFocusNote().getContextualPath(),
          contains(expectedGrandParentUriAndTitle, expectedParentUriAndTitle));
    }

    @Test
    void shouldIncludeNonParentAncestorsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      List<BareNote> contextualNotes =
          result.getRelatedNotes().stream()
              .filter(
                  n -> n.getRelationToFocusNote() == RelationshipToFocusNote.NoteInContextualPath)
              .collect(Collectors.toList());

      assertThat(
          contextualNotes, hasSize(1)); // Only grandparent, parent is already added as Parent
      assertThat(contextualNotes.get(0).getUriAndTitle(), equalTo(expectedGrandParentUriAndTitle));
    }
  }

  @Nested
  class WhenNoteHasPriorSiblings {
    private Note parent;
    private Note priorSibling1;
    private Note priorSibling2;
    private Note focusNote;
    private String expectedPriorSibling1UriAndTitle;
    private String expectedPriorSibling2UriAndTitle;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().titleConstructor("Parent Note").please();
      priorSibling1 =
          makeMe
              .aNote()
              .under(parent)
              .titleConstructor("Prior One")
              .details("Sibling 1 Details")
              .please();
      priorSibling2 =
          makeMe
              .aNote()
              .under(parent)
              .titleConstructor("Prior Two")
              .details("Sibling 2 Details")
              .please();
      focusNote = makeMe.aNote().under(parent).titleConstructor("Focus Note").please();
      expectedPriorSibling1UriAndTitle = "[Prior One](/n" + priorSibling1.getId() + ")";
      expectedPriorSibling2UriAndTitle = "[Prior Two](/n" + priorSibling2.getId() + ")";
    }

    @Test
    void shouldIncludePriorSiblingsInFocusNoteListInOrder() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      assertThat(
          result.getFocusNote().getPriorSiblings(),
          contains(expectedPriorSibling1UriAndTitle, expectedPriorSibling2UriAndTitle));
    }

    @Test
    void shouldIncludePriorSiblingsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      List<BareNote> siblingNotes =
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.PriorSibling)
              .collect(Collectors.toList());

      assertThat(siblingNotes, hasSize(2));
      assertThat(
          siblingNotes.stream().map(BareNote::getUriAndTitle).collect(Collectors.toList()),
          containsInAnyOrder(expectedPriorSibling1UriAndTitle, expectedPriorSibling2UriAndTitle));
    }
  }

  @Nested
  class WhenNoteHasReifiedChildObject {
    private Note focusNote;
    private Note reifiedChild;
    private Note targetNote;
    private String expectedChildUriAndTitle;
    private String expectedTargetUriAndTitle;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().titleConstructor("Focus Note").please();

      // Create the target note first
      targetNote =
          makeMe.aNote().titleConstructor("Target Note").details("Target Details").please();

      // Create a link between parent and target
      reifiedChild = makeMe.aLink().between(focusNote, targetNote).please();
      makeMe.refresh(reifiedChild);

      expectedChildUriAndTitle =
          "[" + reifiedChild.getTopicConstructor() + "](/n" + reifiedChild.getId() + ")";
      expectedTargetUriAndTitle = "[Target Note](/n" + targetNote.getId() + ")";
    }

    @Test
    void shouldIncludeChildObjectInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      assertRelatedNotesContain(
          result, RelationshipToFocusNote.ReifiedChildObject, expectedTargetUriAndTitle);

      // Child should still be in children list
      assertThat(result.getFocusNote().getChildren(), contains(expectedChildUriAndTitle));
    }

    @Nested
    class WhenHasMultipleRegularChildren {
      private Note regularChild1;
      private Note regularChild2;
      private Note regularChild3;
      private String expectedRegularChild1UriAndTitle;
      private String expectedRegularChild2UriAndTitle;
      private String expectedRegularChild3UriAndTitle;
      private String expectedChildUriAndTitle;
      private String expectedTargetUriAndTitle;

      @BeforeEach
      void setup() {
        // Add three regular children
        regularChild1 =
            makeMe.aNote().under(focusNote).titleConstructor("Regular Child 1").please();
        regularChild2 =
            makeMe.aNote().under(focusNote).titleConstructor("Regular Child 2").please();
        regularChild3 =
            makeMe.aNote().under(focusNote).titleConstructor("Regular Child 3").please();

        makeMe.refresh(focusNote);
        expectedRegularChild1UriAndTitle = getUriAndTitle(regularChild1);
        expectedRegularChild2UriAndTitle = getUriAndTitle(regularChild2);
        expectedRegularChild3UriAndTitle = getUriAndTitle(regularChild3);
        expectedChildUriAndTitle = getUriAndTitle(reifiedChild);
        expectedTargetUriAndTitle = getUriAndTitle(targetNote);
      }

      @Test
      void shouldAlternateBetweenPriorityLevelsWhenBudgetIsLimited() {
        // Set budget to allow only 4 notes
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 4);

        // Verify related notes
        List<BareNote> relatedNotes = result.getRelatedNotes();
        assertThat(relatedNotes, hasSize(4));

        // Should have three children
        assertThat(
            result.getFocusNote().getChildren(),
            containsInAnyOrder(
                expectedRegularChild1UriAndTitle,
                expectedRegularChild2UriAndTitle,
                expectedChildUriAndTitle));

        // Verify relationships in order
        assertThat(
            relatedNotes.stream()
                .map(BareNote::getRelationToFocusNote)
                .collect(Collectors.toList()),
            contains(
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.ReifiedChildObject));

        // Verify the reified child object is included
        assertRelatedNotesContain(
            result, RelationshipToFocusNote.ReifiedChildObject, expectedTargetUriAndTitle);
      }

      @Test
      void shouldIncludeAllChildrenWhenBudgetIsEnough() {
        // Set budget to allow all notes
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

        // Verify related notes include all children and the reified child object
        assertThat(
            result.getFocusNote().getChildren(),
            containsInAnyOrder(
                expectedRegularChild1UriAndTitle,
                expectedRegularChild2UriAndTitle,
                expectedRegularChild3UriAndTitle,
                expectedChildUriAndTitle));

        // Verify the reified child object is included
        assertRelatedNotesContain(
            result, RelationshipToFocusNote.ReifiedChildObject, expectedTargetUriAndTitle);
      }

      @Test
      void shouldNotIncludeReifiedChildObjectWhenItComesAfterRegularChildrenAndBudgetIsLimited() {
        // Delete existing reified child
        makeMe.theNote(reifiedChild).after(regularChild3);
        makeMe.refresh(focusNote);

        // Set budget to allow only 4 notes
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 4);

        // Verify related notes
        List<BareNote> relatedNotes = result.getRelatedNotes();
        assertThat(relatedNotes, hasSize(4));

        // Verify relationships are all children
        assertThat(
            relatedNotes.stream()
                .map(BareNote::getRelationToFocusNote)
                .collect(Collectors.toList()),
            contains(
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.Child));

        // Verify no reified child object is included
        assertThat(
            getNotesWithRelationship(result, RelationshipToFocusNote.ReifiedChildObject), empty());
      }
    }
  }

  @Nested
  class WhenNoteHasReferringNotes {
    private Note focusNote;
    private Note referringParent1;
    private Note referringNote1;
    private Note referringParent2;
    private Note referringNote2;
    private String expectedReferringNote1UriAndTitle;
    private String expectedReferringNote2UriAndTitle;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().titleConstructor("Focus Note").details("Focus Details").please();

      // Create first referring note
      referringParent1 = makeMe.aNote().titleConstructor("Referring Parent 1").please();
      referringNote1 = makeMe.aLink().between(referringParent1, focusNote).please();

      // Create second referring note
      referringParent2 = makeMe.aNote().titleConstructor("Referring Parent 2").please();
      referringNote2 = makeMe.aLink().between(referringParent2, focusNote).please();

      expectedReferringNote1UriAndTitle = getUriAndTitle(referringNote1);
      expectedReferringNote2UriAndTitle = getUriAndTitle(referringNote2);
    }

    @Test
    void shouldIncludeReferringNotesInFocusNoteAndRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      // Verify referring notes are in focus note's list
      assertThat(
          result.getFocusNote().getReferrings(),
          containsInAnyOrder(expectedReferringNote1UriAndTitle, expectedReferringNote2UriAndTitle));

      // Verify referring notes are in related notes
      assertRelatedNotesContain(
          result,
          RelationshipToFocusNote.ReferringNote,
          expectedReferringNote1UriAndTitle,
          expectedReferringNote2UriAndTitle);
    }
  }
}
