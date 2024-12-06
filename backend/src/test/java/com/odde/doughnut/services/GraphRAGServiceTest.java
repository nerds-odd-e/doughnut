package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.graphRAG.RelationshipToFocusNote;
import com.odde.doughnut.testability.MakeMe;
import java.util.stream.Collectors;
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

  private String generateLongDetails(int length) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append('x');
    }
    return sb.toString();
  }

  @Test
  void shouldReturnFocusNoteForSingleNoteNotebook() {
    Note note = makeMe.aNote().titleConstructor("Test Note").details("Test Details").please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    assertThat(result.focusNote.uriAndTitle, equalTo("[Test Note](/n" + note.getId() + ")"));
    assertThat(result.focusNote.detailsTruncated, equalTo("Test Details"));
    assertThat(result.relatedNotes, empty());
  }

  @Test
  void shouldIncludeParentAndContextualPathForNoteWithParent() {
    Note parent = makeMe.aNote().titleConstructor("Parent Note").please();
    Note note =
        makeMe.aNote().titleConstructor("Test Note").under(parent).details("Test Details").please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    assertThat(
        result.focusNote.parentUriAndTitle,
        equalTo(String.format("[Parent Note](/n%d)", parent.getId())));
    assertThat(result.focusNote.contextualPath, hasSize(1));
    assertThat(
        result.focusNote.contextualPath.get(0),
        equalTo(String.format("[Parent Note](/n%d)", parent.getId())));
    assertThat(
        result.relatedNotes.get(0).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.Parent));
  }

  @Test
  void shouldIncludeObjectForReificationNote() {
    Note parent = makeMe.aNote().titleConstructor("Subject").please();
    String longObjectDetails = generateLongDetails(2000);
    Note target = makeMe.aNote().titleConstructor("Object").details(longObjectDetails).please();
    Note note = makeMe.aLink().between(parent, target).please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    assertThat(
        result.focusNote.objectUriAndTitle,
        equalTo(String.format("[Object](/n%d)", target.getId())));

    assertThat(result.relatedNotes, hasSize(2));
    assertThat(
        result.relatedNotes.get(0).uriAndTitle,
        equalTo(String.format("[Subject](/n%d)", parent.getId())));
    assertThat(
        result.relatedNotes.get(1).uriAndTitle,
        equalTo(String.format("[Object](/n%d)", target.getId())));
    assertThat(
        result.relatedNotes.get(1).detailsTruncated,
        equalTo(
            longObjectDetails.substring(0, GraphRAGService.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH)));
    assertThat(
        result.relatedNotes.get(0).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.Parent));
    assertThat(
        result.relatedNotes.get(1).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.Object));
  }

  @Test
  void shouldIncludeFullContextualPathForDeeplyNestedNote() {
    Note greatGrandparent = makeMe.aNote().titleConstructor("Great Grandparent").please();
    Note grandparent =
        makeMe.aNote().titleConstructor("Grandparent").under(greatGrandparent).please();
    Note parent = makeMe.aNote().titleConstructor("Parent").under(grandparent).please();
    Note note = makeMe.aNote().titleConstructor("Test Note").under(parent).please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    assertThat(result.focusNote.contextualPath, hasSize(3));
    assertThat(
        result.focusNote.contextualPath.get(0),
        equalTo(String.format("[Great Grandparent](/n%d)", greatGrandparent.getId())));
    assertThat(
        result.focusNote.contextualPath.get(1),
        equalTo(String.format("[Grandparent](/n%d)", grandparent.getId())));
    assertThat(
        result.focusNote.contextualPath.get(2),
        equalTo(String.format("[Parent](/n%d)", parent.getId())));

    assertThat(result.relatedNotes, hasSize(3));
    assertThat(
        result.relatedNotes.get(0).uriAndTitle,
        equalTo(String.format("[Great Grandparent](/n%d)", greatGrandparent.getId())));
    assertThat(
        result.relatedNotes.get(1).uriAndTitle,
        equalTo(String.format("[Grandparent](/n%d)", grandparent.getId())));
    assertThat(
        result.relatedNotes.get(2).uriAndTitle,
        equalTo(String.format("[Parent](/n%d)", parent.getId())));
    assertThat(
        result.relatedNotes.get(0).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.Ancestor));
    assertThat(
        result.relatedNotes.get(1).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.Ancestor));
    assertThat(
        result.relatedNotes.get(2).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.Parent));
  }

  @Test
  void shouldNotDuplicateParentInRelatedNotes() {
    Note parent = makeMe.aNote().titleConstructor("Parent").please();
    Note note = makeMe.aNote().titleConstructor("Test Note").under(parent).please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    assertThat(result.relatedNotes, hasSize(1));
    assertThat(
        result.relatedNotes.get(0).uriAndTitle,
        equalTo(String.format("[Parent](/n%d)", parent.getId())));
  }

  @Test
  void shouldIncludeObjectContextualPathForReificationNote() {
    Note objectGrandparent = makeMe.aNote().titleConstructor("Object Grandparent").please();
    Note objectParent =
        makeMe.aNote().titleConstructor("Object Parent").under(objectGrandparent).please();
    Note target = makeMe.aNote().titleConstructor("Object").under(objectParent).please();
    Note subject = makeMe.aNote().titleConstructor("Subject").please();
    Note note = makeMe.aLink().between(subject, target).please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    // Check object's ancestors are in related notes
    assertThat(result.relatedNotes, hasSize(4)); // subject + target + target's 2 ancestors
    assertThat(
        result.relatedNotes.get(0).uriAndTitle,
        equalTo(String.format("[Subject](/n%d)", subject.getId())));
    assertThat(
        result.relatedNotes.get(1).uriAndTitle,
        equalTo(String.format("[Object](/n%d)", target.getId())));
    assertThat(
        result.relatedNotes.get(2).uriAndTitle,
        equalTo(String.format("[Object Grandparent](/n%d)", objectGrandparent.getId())));
    assertThat(
        result.relatedNotes.get(3).uriAndTitle,
        equalTo(String.format("[Object Parent](/n%d)", objectParent.getId())));
    assertThat(
        result.relatedNotes.get(0).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.Parent));
    assertThat(
        result.relatedNotes.get(1).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.Object));
    assertThat(
        result.relatedNotes.get(2).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.ObjectAncestor));
    assertThat(
        result.relatedNotes.get(3).relationshipToFocusNote,
        equalTo(RelationshipToFocusNote.ObjectAncestor));
  }

  @Test
  void shouldNotIncludeChildrenWhenTokenBudgetIsZero() {
    Note parent = makeMe.aNote().titleConstructor("Parent").please();
    Note child = makeMe.aNote().titleConstructor("Child").under(parent).please();

    GraphRAGResult result = graphRAGService.retrieve(parent, 0);

    // Children should not be in focus note's children list
    assertThat(result.focusNote.children, empty());

    // Child should not be in related notes (only Priority 1 items in related notes)
    assertThat(
        result.relatedNotes.stream()
            .map(note -> note.uriAndTitle)
            .noneMatch(uri -> uri.contains("Child")),
        equalTo(true));
  }

  @Test
  void shouldIncludeChildrenWhenTokenBudgetIsLarge() {
    Note parent = makeMe.aNote().titleConstructor("Parent").please();
    Note child1 = makeMe.aNote().titleConstructor("First Child").under(parent).please();
    Note child2 = makeMe.aNote().titleConstructor("Second Child").under(parent).please();

    GraphRAGResult result = graphRAGService.retrieve(parent, Integer.MAX_VALUE);

    // Children should be in focus note's children list
    assertThat(result.focusNote.children, hasSize(2));
    assertThat(
        result.focusNote.children.get(0),
        equalTo(String.format("[First Child](/n%d)", child1.getId())));
    assertThat(
        result.focusNote.children.get(1),
        equalTo(String.format("[Second Child](/n%d)", child2.getId())));

    // Children should also be in related notes
    assertThat(
        result.relatedNotes.stream().map(note -> note.uriAndTitle).collect(Collectors.toList()),
        hasItems(
            String.format("[First Child](/n%d)", child1.getId()),
            String.format("[Second Child](/n%d)", child2.getId())));
    assertThat(
        result.relatedNotes.stream()
            .map(note -> note.relationshipToFocusNote)
            .collect(Collectors.toList()),
        everyItem(equalTo(RelationshipToFocusNote.Child)));
  }

  @Test
  void shouldIncludeChildWhenTokenBudgetJustEnough() {
    Note parent = makeMe.aNote().titleConstructor("Parent").please();
    Note child = makeMe.aNote().titleConstructor("Child").details("D").under(parent).please();

    int requiredTokens = 25; // Base token requirement

    // Test with exactly enough tokens
    GraphRAGResult result = graphRAGService.retrieve(parent, requiredTokens);

    // Child should be included
    assertThat(result.focusNote.children, hasSize(1));
    assertThat(
        result.focusNote.children.get(0), equalTo(String.format("[Child](/n%d)", child.getId())));
    assertThat(
        result.relatedNotes.stream().map(note -> note.uriAndTitle).collect(Collectors.toList()),
        hasItem(String.format("[Child](/n%d)", child.getId())));

    // Test with one token less
    result = graphRAGService.retrieve(parent, requiredTokens - 1);
    assertThat(result.focusNote.children, empty());
    assertThat(
        result.relatedNotes.stream()
            .map(note -> note.uriAndTitle)
            .noneMatch(uri -> uri.contains("Child")),
        equalTo(true));
  }

  @Test
  void shouldIncludeOnlyFirstChildWhenTokenBudgetOnlyAllowsOne() {
    Note parent = makeMe.aNote().titleConstructor("Parent").please();
    Note child1 =
        makeMe.aNote().titleConstructor("First Child").details("D1").under(parent).please();
    makeMe.aNote().titleConstructor("Second Child").details("D2").under(parent).please();

    // Test with budget enough for only one child
    GraphRAGResult result = graphRAGService.retrieve(parent, 27);

    // Only first child should be in focus note's children list
    assertThat(result.focusNote.children, hasSize(1));
    assertThat(
        result.focusNote.children.get(0),
        equalTo(String.format("[First Child](/n%d)", child1.getId())));

    // Only first child should be in related notes
    assertThat(result.relatedNotes, hasSize(1));
    assertThat(
        result.relatedNotes.get(0).uriAndTitle,
        equalTo(String.format("[First Child](/n%d)", child1.getId())));

    // Test with budget enough for both children
    result = graphRAGService.retrieve(parent, 56);
    assertThat(result.focusNote.children, hasSize(2));
    assertThat(result.relatedNotes, hasSize(2));
  }
}
