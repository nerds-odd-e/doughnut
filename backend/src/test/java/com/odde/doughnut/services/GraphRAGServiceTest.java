package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.graphRAG.RelationshipToFocusNote;
import com.odde.doughnut.testability.MakeMe;
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
      assertThat(result.getFocusNote().getContextualPath(), contains(expectedParentUriAndTitle));
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
      GraphRAGResult result = graphRAGService.retrieve(note, 5);

      assertThat(result.getFocusNote().getParentUriAndTitle(), equalTo(expectedParentUriAndTitle));
      assertThat(result.getFocusNote().getContextualPath(), contains(expectedParentUriAndTitle));
      assertThat(result.getRelatedNotes(), empty());
    }
  }
}
