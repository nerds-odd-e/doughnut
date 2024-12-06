package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.graphRAG.RelationshipToFocusNote;
import com.odde.doughnut.testability.MakeMe;
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

  private final GraphRAGService graphRAGService = new GraphRAGService();

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

  @Test
  void shouldIncludeParentInContextualPathOfFocusNote() {
    Note parent = makeMe.aNote().titleConstructor("Parent Note").please();
    Note note = makeMe.aNote().under(parent).please();

    GraphRAGResult result = graphRAGService.retrieve(note, 1000);

    assertThat(result.getFocusNote().getContextualPath(), hasSize(1));
    assertThat(
        result.getFocusNote().getContextualPath().get(0),
        equalTo("[Parent Note](/n" + parent.getId() + ")"));
  }

  @Test
  void shouldIncludeParentInRelatedNotesWhenBudgetAllows() {
    Note parent = makeMe.aNote().titleConstructor("Parent Note").details("Parent Details").please();
    Note note = makeMe.aNote().under(parent).please();

    GraphRAGResult result = graphRAGService.retrieve(note, 1000);

    assertThat(result.getRelatedNotes(), hasSize(1));
    assertThat(
        result.getRelatedNotes().get(0).getRelationToFocusNote(),
        equalTo(RelationshipToFocusNote.Parent));
  }
}
