package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
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

  @Autowired private GraphRAGService graphRAGService;

  @Test
  void shouldReturnFocusNoteForSingleNoteNotebook() {
    Note note = makeMe.aNote().titleConstructor("Test Note").details("Test Details").please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    assertThat(result.focusNote.uriAndTitle, equalTo("[Test Note](/n" + note.getId() + ")"));
    assertThat(result.focusNote.detailsTruncated, equalTo("Test Details"));
    assertThat(result.focusNote.parentUriAndTitle, nullValue());
    assertThat(result.focusNote.objectUriAndTitle, nullValue());
    assertThat(result.focusNote.contextualPath, empty());
    assertThat(result.focusNote.children, empty());
    assertThat(result.focusNote.referrings, empty());
    assertThat(result.focusNote.priorSiblings, empty());
    assertThat(result.focusNote.youngerSiblings, empty());
    assertThat(result.relatedNotes, empty());
  }

  @Test
  void shouldIncludeParentAndContextualPathForNoteWithParent() {
    Note parent = makeMe.aNote().titleConstructor("Parent Note").please();
    Note note =
        makeMe.aNote().titleConstructor("Test Note").under(parent).details("Test Details").please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    // Check parent relationship
    assertThat(
        result.focusNote.parentUriAndTitle,
        equalTo(String.format("[Parent Note](/n%d)", parent.getId())));

    // Check contextual path
    assertThat(result.focusNote.contextualPath, hasSize(1));
    assertThat(
        result.focusNote.contextualPath.get(0),
        equalTo(String.format("[Parent Note](/n%d)", parent.getId())));

    // Other properties should remain as before
    assertThat(
        result.focusNote.uriAndTitle, equalTo(String.format("[Test Note](/n%d)", note.getId())));
    assertThat(result.focusNote.detailsTruncated, equalTo("Test Details"));
    assertThat(result.focusNote.objectUriAndTitle, nullValue());
    assertThat(result.focusNote.children, empty());
    assertThat(result.focusNote.referrings, empty());
    assertThat(result.focusNote.priorSiblings, empty());
    assertThat(result.focusNote.youngerSiblings, empty());
    assertThat(result.relatedNotes, empty());
  }
}
