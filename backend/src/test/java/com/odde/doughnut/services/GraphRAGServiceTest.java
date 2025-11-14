package com.odde.doughnut.services;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
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

  private final GraphRAGService graphRAGService =
      new GraphRAGService(new OneTokenPerNoteStrategy());

  // Helper methods for common test operations
  private List<BareNote> getNotesWithRelationship(
      GraphRAGResult result, RelationshipToFocusNote relationship) {
    return result.getRelatedNotes().stream()
        .filter(n -> n.getRelationToFocusNote() == relationship)
        .collect(Collectors.toList());
  }

  private void assertRelatedNotesContain(
      GraphRAGResult result, RelationshipToFocusNote relationship, Note... expectedNotes) {
    List<BareNote> notes = getNotesWithRelationship(result, relationship);
    assertThat(notes, hasSize(expectedNotes.length));
    assertThat(notes, containsInAnyOrder((Object[]) expectedNotes));
  }

  @Nested
  class SimpleNoteWithNoParentOrChild {
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
  }

  @Nested
  class WhenNoteHasParent {
    private Note parent;
    private Note note;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().titleConstructor("Parent Note").details("Parent Details").please();
      note = makeMe.aNote().under(parent).please();
    }

    @Test
    void shouldIncludeParentInFocusNoteAndContextualPath() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      assertThat(result.getFocusNote().getParentUriAndTitle(), equalTo(parent));
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

      assertThat(result.getFocusNote().getParentUriAndTitle(), equalTo(parent));
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
    private Note object;
    private Note note;

    @BeforeEach
    void setup() {
      Note parent = makeMe.aNote().titleConstructor("Parent Note").please();
      object = makeMe.aNote().titleConstructor("Object Note").details("Object Details").please();
      note = makeMe.aReification().between(parent, object).please();
    }

    @Test
    void shouldIncludeObjectInFocusNote() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      assertThat(result.getFocusNote().getObjectUriAndTitle(), equalTo(object));
    }

    @Test
    void shouldIncludeObjectInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      assertThat(result.getRelatedNotes(), hasSize(2));
      assertThat(
          result.getRelatedNotes().stream()
              .filter(n -> n.getRelationToFocusNote() == RelationshipToFocusNote.Object)
              .findFirst()
              .get(),
          equalTo(object));
    }

    @Test
    void shouldKeepObjectInFocusNoteEvenWhenBudgetOnlyAllowsParent() {
      GraphRAGResult result = graphRAGService.retrieve(note, 2); // Only enough for parent

      // Object URI should still be in focus note
      assertThat(result.getFocusNote().getObjectUriAndTitle(), equalTo(object));

      // Only parent should be in related notes
      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getRelationToFocusNote(),
          equalTo(RelationshipToFocusNote.Parent));
    }

    @Test
    void shouldNotDuplicateNoteInRelatedNotesWhenItIsAlsoAChild() {
      // Create a child note that is also the object of the focus note
      makeMe.theNote(object).under(note).please();
      makeMe.refresh(note);

      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      // Should be in both object and children lists of focus note
      assertThat(result.getFocusNote().getObjectUriAndTitle(), equalTo(object));
      assertThat(result.getFocusNote().getChildren(), contains(object));

      // But should appear only once in related notes
      assertThat(result.getRelatedNotes(), hasSize(3)); // parent and child/object
    }

    @Nested
    class WhenObjectHasContextualPath {
      private Note objectGrandParent;
      private Note objectParent;

      @BeforeEach
      void setup() {
        objectGrandParent = makeMe.aNote().titleConstructor("Object Grand Parent").please();
        objectParent =
            makeMe.aNote().under(objectGrandParent).titleConstructor("Object Parent").please();
        makeMe.theNote(object).under(objectParent).please();
        makeMe.refresh(object);
      }

      @Test
      void shouldIncludeObjectContextualPathInRelatedNotes() {
        GraphRAGResult result = graphRAGService.retrieve(note, 1000);

        // Verify object's contextual path notes are in related notes
        assertRelatedNotesContain(
            result,
            RelationshipToFocusNote.AncestorInObjectContextualPath,
            objectGrandParent,
            objectParent);
      }

      @Test
      void shouldNotIncludeObjectContextualPathWhenBudgetIsLimited() {
        // Set budget to only allow object
        GraphRAGResult result = graphRAGService.retrieve(note, 3);

        // Verify object is included but not its contextual path
        assertThat(
            result.getRelatedNotes().stream()
                .map(BareNote::getRelationToFocusNote)
                .collect(Collectors.toList()),
            contains(RelationshipToFocusNote.Parent, RelationshipToFocusNote.Object));

        // Verify no object contextual path notes are included
        assertThat(
            getNotesWithRelationship(
                result, RelationshipToFocusNote.AncestorInObjectContextualPath),
            empty());
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
    }

    @Test
    void shouldIncludeChildrenInFocusNoteList() {
      GraphRAGResult result = graphRAGService.retrieve(parent, 1000);

      assertThat(result.getFocusNote().getChildren(), containsInAnyOrder(child1, child2));
    }

    @Test
    void shouldIncludeChildrenInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(parent, 1000);

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
      GraphRAGResult result = graphRAGService.retrieve(parent, 2);

      // Only child1 should be in focus note's children list
      assertThat(result.getFocusNote().getChildren(), contains(child1));

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
      Note parent = makeMe.aNote().titleConstructor("Parent Note").please();
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
    }

    @Test
    void shouldIncludeYoungerSiblingsInFocusNoteList() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      assertThat(
          result.getFocusNote().getYoungerSiblings(), contains(youngerSibling1, youngerSibling2));
    }

    @Test
    void shouldIncludeYoungerSiblingsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      assertRelatedNotesContain(
          result, RelationshipToFocusNote.YoungerSibling, youngerSibling1, youngerSibling2);
    }

    @Nested
    class AndAlsoHasChildren {
      private Note child1;

      @BeforeEach
      void setup() {
        child1 =
            makeMe
                .aNote()
                .under(focusNote)
                .titleConstructor("Child One")
                .details("Child 1 Details")
                .please();
        makeMe
            .aNote()
            .under(focusNote)
            .titleConstructor("Child Two")
            .details("Child 2 Details")
            .please();
      }

      @Test
      void shouldAlternateBetweenChildrenAndYoungerSiblingsWhenBudgetIsLimited() {
        // Set budget to only allow two notes
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 4);

        // Verify in related notes
        List<BareNote> relatedNotes = result.getRelatedNotes();
        assertThat(relatedNotes, hasSize(3));

        // Should have one child and one younger sibling
        assertThat(result.getFocusNote().getChildren(), containsInAnyOrder(child1));
        assertThat(result.getFocusNote().getYoungerSiblings(), contains(youngerSibling1));

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
      grandParent = makeMe.aNote().titleConstructor("Grand Parent").details("GP Details").please();
      parent =
          makeMe
              .aNote()
              .under(grandParent)
              .titleConstructor("Parent")
              .details("Parent Details")
              .please();
      focusNote = makeMe.aNote().under(parent).titleConstructor("Focus").please();
    }

    @Test
    void shouldIncludeAncestorsInContextualPathInOrder() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 0);

      // Should be in contextual path in order from root to parent
      assertThat(
          result.getFocusNote().getContextualPath().stream()
              .map(UriAndTitle::getNote)
              .collect(Collectors.toList()),
          contains(grandParent, parent));
    }

    @Test
    void shouldIncludeNonParentAncestorsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      List<BareNote> contextualNotes =
          result.getRelatedNotes().stream()
              .filter(
                  n ->
                      n.getRelationToFocusNote()
                          == RelationshipToFocusNote.AncestorInContextualPath)
              .collect(Collectors.toList());

      assertThat(
          contextualNotes, hasSize(1)); // Only grandparent, parent is already added as Parent
      assertThat(contextualNotes.get(0), equalTo(grandParent));
    }
  }

  @Nested
  class WhenNoteHasPriorSiblings {
    private Note priorSibling1;
    private Note priorSibling2;
    private Note focusNote;

    @BeforeEach
    void setup() {
      Note parent = makeMe.aNote().titleConstructor("Parent Note").please();
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
    }

    @Test
    void shouldIncludePriorSiblingsInFocusNoteListInOrder() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      assertThat(result.getFocusNote().getPriorSiblings(), contains(priorSibling1, priorSibling2));
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
          containsInAnyOrder(priorSibling1, priorSibling2));
    }
  }

  @Nested
  class WhenNoteHasReifiedChildObject {
    private Note focusNote;
    private Note reifiedChild;
    private Note objectNote;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().titleConstructor("Focus Note").please();

      // Create the object note first
      objectNote =
          makeMe.aNote().titleConstructor("Object Note").details("Object Details").please();

      // Create a link between parent and object
      reifiedChild = makeMe.aReification().between(focusNote, objectNote).please();
      makeMe.refresh(reifiedChild);
    }

    @Test
    void shouldIncludeChildObjectInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      assertRelatedNotesContain(result, RelationshipToFocusNote.ObjectOfReifiedChild, objectNote);

      // Child should still be in children list
      assertThat(result.getFocusNote().getChildren(), contains(UriAndTitle.fromNote(reifiedChild)));
    }

    @Nested
    class WhenHasMultipleRegularChildren {
      private Note regularChild1;
      private Note regularChild2;
      private Note regularChild3;

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
      }

      @Test
      void shouldAlternateBetweenPriorityLevelsWhenBudgetIsLimited() {
        // Set budget to allow only 4 notes
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 5);

        // Verify related notes
        List<BareNote> relatedNotes = result.getRelatedNotes();
        assertThat(relatedNotes, hasSize(4));

        // Should have three children
        assertThat(
            result.getFocusNote().getChildren(),
            containsInAnyOrder(regularChild1, regularChild2, reifiedChild));

        // Verify relationships in order
        assertThat(
            relatedNotes.stream()
                .map(BareNote::getRelationToFocusNote)
                .collect(Collectors.toList()),
            contains(
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.Child,
                RelationshipToFocusNote.ObjectOfReifiedChild));

        // Verify the reified child object is included
        assertRelatedNotesContain(result, RelationshipToFocusNote.ObjectOfReifiedChild, objectNote);
      }

      @Test
      void shouldIncludeAllChildrenWhenBudgetIsEnough() {
        // Set budget to allow all notes
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

        // Verify related notes include all children and the reified child object
        assertThat(
            result.getFocusNote().getChildren(),
            containsInAnyOrder(regularChild1, regularChild2, regularChild3, reifiedChild));

        // Verify the reified child object is included
        assertRelatedNotesContain(result, RelationshipToFocusNote.ObjectOfReifiedChild, objectNote);
      }

      @Test
      void shouldNotIncludeReifiedChildObjectWhenItComesAfterRegularChildrenAndBudgetIsLimited() {
        // Delete existing reified child
        makeMe.theNote(reifiedChild).after(regularChild3);
        makeMe.refresh(focusNote);

        // Set budget to allow only 4 notes
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 5);

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
            getNotesWithRelationship(result, RelationshipToFocusNote.ObjectOfReifiedChild),
            empty());
      }
    }
  }

  @Nested
  class WhenNoteHasInboundReferenceNotes {
    private Note focusNote;
    private Note inboundReferenceParent1;
    private Note inboundReferenceNote1;
    private Note inboundReferenceParent2;
    private Note inboundReferenceNote2;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().titleConstructor("Focus Note").details("Focus Details").please();

      // Create first inbound reference note
      inboundReferenceParent1 =
          makeMe.aNote().titleConstructor("Inbound Reference Parent 1").please();
      inboundReferenceNote1 =
          makeMe.aReification().between(inboundReferenceParent1, focusNote).please();

      // Create second inbound reference note
      inboundReferenceParent2 =
          makeMe.aNote().titleConstructor("Inbound Reference Parent 2").please();
      inboundReferenceNote2 =
          makeMe.aReification().between(inboundReferenceParent2, focusNote).please();
    }

    @Test
    void shouldIncludeInboundReferenceNotesAndTheirSubjectsWhenBudgetIsEnough() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      // Verify inbound reference notes are in focus note's list
      assertThat(
          result.getFocusNote().getInboundReferences().stream()
              .map(UriAndTitle::getNote)
              .collect(Collectors.toList()),
          containsInAnyOrder(inboundReferenceNote1, inboundReferenceNote2));

      // Verify inbound reference notes are in related notes
      assertRelatedNotesContain(
          result,
          RelationshipToFocusNote.InboundReference,
          inboundReferenceNote1,
          inboundReferenceNote2);

      // Verify inbound reference subjects are in related notes
      assertRelatedNotesContain(
          result,
          RelationshipToFocusNote.SubjectOfInboundReference,
          inboundReferenceParent1,
          inboundReferenceParent2);
    }

    @Test
    void shouldNotIncludeInboundReferenceSubjectsWhenBudgetIsLimited() {
      // Set budget to only allow inbound reference notes
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 3);

      // Verify only inbound reference notes are included
      assertThat(result.getRelatedNotes(), hasSize(2));
      assertThat(
          result.getRelatedNotes().stream()
              .map(BareNote::getRelationToFocusNote)
              .collect(Collectors.toList()),
          everyItem(equalTo(RelationshipToFocusNote.InboundReference)));

      // Verify no inbound reference subjects are included
      assertThat(
          getNotesWithRelationship(result, RelationshipToFocusNote.SubjectOfInboundReference),
          empty());
    }
  }

  @Nested
  class WhenNoteHasParentSiblings {
    private Note parentSibling1;
    private Note parentSibling2;
    private Note focusNote;

    @BeforeEach
    void setup() {
      Note grandParent = makeMe.aNote().titleConstructor("Grand Parent").please();
      Note parent = makeMe.aNote().under(grandParent).titleConstructor("Parent").please();
      parentSibling1 =
          makeMe.aNote().under(grandParent).titleConstructor("Parent Sibling 1").please();
      parentSibling2 =
          makeMe.aNote().under(grandParent).titleConstructor("Parent Sibling 2").please();
      focusNote = makeMe.aNote().under(parent).titleConstructor("Focus Note").please();
    }

    @Test
    void shouldIncludeParentSiblingsInRelatedNotes() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      // Verify parent siblings are in related notes
      assertRelatedNotesContain(
          result, RelationshipToFocusNote.SiblingOfParent, parentSibling1, parentSibling2);
    }

    @Test
    void shouldNotIncludeParentSiblingsWhenBudgetIsLimited() {
      // Set budget to only allow parent
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 2);

      // Verify only parent is included
      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getRelationToFocusNote(),
          equalTo(RelationshipToFocusNote.Parent));

      // Verify no parent siblings are included
      assertThat(
          getNotesWithRelationship(result, RelationshipToFocusNote.SiblingOfParent), empty());
    }

    @Nested
    class WhenParentSiblingsHaveChildren {
      private Note parentSibling1Child1;
      private Note parentSibling1Child2;
      private Note parentSibling2Child1;

      @BeforeEach
      void setup() {
        parentSibling1Child1 =
            makeMe.aNote().under(parentSibling1).titleConstructor("PS1 Child 1").please();
        parentSibling1Child2 =
            makeMe.aNote().under(parentSibling1).titleConstructor("PS1 Child 2").please();
        parentSibling2Child1 =
            makeMe.aNote().under(parentSibling2).titleConstructor("PS2 Child 1").please();
      }

      @Test
      void shouldIncludeParentSiblingChildrenInRelatedNotes() {
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

        // Verify parent sibling children are in related notes
        assertRelatedNotesContain(
            result,
            RelationshipToFocusNote.ChildOfSiblingOfParent,
            parentSibling1Child1,
            parentSibling1Child2,
            parentSibling2Child1);
      }

      @Test
      void shouldNotIncludeParentSiblingChildrenWhenBudgetIsLimited() {
        // Set budget to only allow parent, parent siblings, and contextual path
        GraphRAGResult result = graphRAGService.retrieve(focusNote, 5);

        // Verify only parent, parent siblings, and contextual path are included
        assertThat(
            result.getRelatedNotes().stream()
                .map(BareNote::getRelationToFocusNote)
                .collect(Collectors.toList()),
            containsInAnyOrder(
                RelationshipToFocusNote.Parent,
                RelationshipToFocusNote.SiblingOfParent,
                RelationshipToFocusNote.SiblingOfParent,
                RelationshipToFocusNote.AncestorInContextualPath));

        // Verify no parent sibling children are included
        assertThat(
            getNotesWithRelationship(result, RelationshipToFocusNote.ChildOfSiblingOfParent),
            empty());
      }
    }
  }

  @Nested
  class JsonSerialization {
    @Test
    void shouldIncludeRelatedNotesFieldInJson() throws Exception {
      Note note = makeMe.aNote().titleConstructor("Test Note").please();
      Note child = makeMe.aNote().under(note).titleConstructor("Child Note").please();

      GraphRAGResult result = graphRAGService.retrieve(note, 1000);

      ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
      JsonNode jsonNode = objectMapper.valueToTree(result);

      assertThat(jsonNode.has("relatedNotes"), is(true));
      assertThat(jsonNode.get("relatedNotes").isArray(), is(true));
      assertThat(jsonNode.get("relatedNotes").size(), equalTo(1));

      // Verify the structure of the related note
      JsonNode relatedNote = jsonNode.get("relatedNotes").get(0);
      assertThat(relatedNote.has("relationToFocusNote"), is(true));
    }
  }

  @Nested
  class WhenObjectOfReifiedChildHasInboundReferences {
    private Note focusNote;
    private Note reifiedChild;
    private Note objectNote;
    private Note inboundReference1;
    private Note inboundReference2;

    @BeforeEach
    void setup() {
      focusNote = makeMe.aNote().titleConstructor("Focus Note").please();

      // Create the object note first
      objectNote =
          makeMe.aNote().titleConstructor("Object Note").details("Object Details").please();

      // Create a link between parent and object
      reifiedChild = makeMe.aReification().between(focusNote, objectNote).please();

      // Create inbound references to the object note
      Note referenceParent1 = makeMe.aNote().titleConstructor("Reference Parent 1").please();
      inboundReference1 = makeMe.aReification().between(referenceParent1, objectNote).please();

      Note referenceParent2 = makeMe.aNote().titleConstructor("Reference Parent 2").please();
      inboundReference2 = makeMe.aReification().between(referenceParent2, objectNote).please();

      makeMe.refresh(objectNote);
    }

    @Test
    void shouldIncludeInboundReferencesToObjectOfReifiedChild() {
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 1000);

      // Verify inbound references to object are included
      assertRelatedNotesContain(
          result,
          RelationshipToFocusNote.InboundReferenceToObjectOfReifiedChild,
          inboundReference1,
          inboundReference2);
    }

    @Test
    void shouldNotIncludeInboundReferencesToObjectWhenBudgetIsLimited() {
      // Set budget to only allow up to object of reified child
      GraphRAGResult result = graphRAGService.retrieve(focusNote, 3);

      // Verify no inbound references to object are included
      assertThat(
          getNotesWithRelationship(
              result, RelationshipToFocusNote.InboundReferenceToObjectOfReifiedChild),
          empty());
    }
  }

  @Nested
  class TruncateDetailsTests {
    @Test
    void shouldTruncateASCIICharactersCorrectly() {
      String longDetails = "a".repeat(2000);
      Note note = makeMe.aNote().titleConstructor("Test Note").details(longDetails).please();
      Note child = makeMe.aNote().under(note).please();

      GraphRAGResult result = graphRAGService.retrieve(child, 1000);

      assertThat(result.getRelatedNotes(), hasSize(1));
      assertThat(
          result.getRelatedNotes().get(0).getDetails(),
          equalTo("a".repeat(RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) + "..."));
    }

    @Test
    void shouldTruncateCJKCharactersCorrectly() {
      // Each CJK character takes 3 bytes in UTF-8
      String cjkText = "你好世界".repeat(500); // 2000 bytes (500 * 4 chars * 3 bytes)
      Note note = makeMe.aNote().titleConstructor("Test Note").details(cjkText).please();
      Note child = makeMe.aNote().under(note).please();

      GraphRAGResult result = graphRAGService.retrieve(child, 1000);

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
      Note note = makeMe.aNote().titleConstructor("Test Note").details(mixedText).please();
      Note child = makeMe.aNote().under(note).please();

      GraphRAGResult result = graphRAGService.retrieve(child, 1000);

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
}
