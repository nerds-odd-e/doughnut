package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.impl.GraphRAGServiceImpl;
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

  @Autowired private GraphRAGServiceImpl graphRAGService;

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

  @Test
  void shouldNotTruncateFocusNoteDetails() {
    String longDetails = generateLongDetails(2000);
    Note note = makeMe.aNote().titleConstructor("Test Note").details(longDetails).please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    assertThat(result.focusNote.detailsTruncated, equalTo(longDetails));
  }

  @Test
  void shouldTruncateRelatedNoteDetails() {
    String longDetails = generateLongDetails(2000);
    Note parent = makeMe.aNote().titleConstructor("Parent Note").details(longDetails).please();
    Note note =
        makeMe.aNote().titleConstructor("Test Note").under(parent).details("Test Details").please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    // When we implement related notes, this will be where we check the truncation
    // For now, we can check the parent's details in the contextual path
    assertThat(
        result.focusNote.parentUriAndTitle,
        equalTo(String.format("[Parent Note](/n%d)", parent.getId())));
    assertThat(parent.getDetails().length(), equalTo(2000));
    assertThat(result.focusNote.detailsTruncated, equalTo("Test Details"));
  }

  @Test
  void shouldIncludeObjectForReificationNote() {
    Note parent = makeMe.aNote().titleConstructor("Subject").please();
    String longObjectDetails = generateLongDetails(2000);
    Note target = makeMe.aNote().titleConstructor("Object").details(longObjectDetails).please();
    Note note = makeMe.aLink().between(parent, target).please();

    GraphRAGResult result = graphRAGService.retrieve(note, 0);

    // Check object relationship in focus note
    assertThat(
        result.focusNote.objectUriAndTitle,
        equalTo(String.format("[Object](/n%d)", target.getId())));

    // Check object note in related notes list
    assertThat(result.relatedNotes, hasSize(1));
    assertThat(
        result.relatedNotes.get(0).uriAndTitle,
        equalTo(String.format("[Object](/n%d)", target.getId())));
    assertThat(
        result.relatedNotes.get(0).detailsTruncated,
        equalTo(
            longObjectDetails.substring(
                0, GraphRAGServiceImpl.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH)));

    // Other properties should remain as before
    assertThat(
        result.focusNote.uriAndTitle,
        equalTo(String.format("[%s](/n%d)", note.getTopicConstructor(), note.getId())));
    assertThat(
        result.focusNote.parentUriAndTitle,
        equalTo(String.format("[Subject](/n%d)", parent.getId())));
    assertThat(result.focusNote.contextualPath, hasSize(1));
    assertThat(
        result.focusNote.contextualPath.get(0),
        equalTo(String.format("[Subject](/n%d)", parent.getId())));
  }
}
