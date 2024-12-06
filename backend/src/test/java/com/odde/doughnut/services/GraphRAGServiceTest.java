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

    assertThat(result.getFocusNote(), notNullValue());
    assertThat(
        result.getFocusNote().getUriAndTitle(), equalTo("[Test Note](/n" + note.getId() + ")"));
    assertThat(result.getFocusNote().getDetails(), equalTo("Test Details"));
    assertThat(
        result.getFocusNote().getRelationToFocusNote(), equalTo(RelationshipToFocusNote.Self));
    assertThat(result.getFocusNote().getParentUriAndTitle(), nullValue());
    assertThat(result.getFocusNote().getObjectUriAndTitle(), nullValue());
    assertThat(result.getRelatedNotes(), empty());
  }
}
