package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GraphRAGResultTest {
  @Autowired private MakeMe makeMe;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final String PARENT_URI_AND_TITLE = "parentUriAndTitle";

  @Test
  void shouldIncludeParentUriAndTitleWhenSerializedToJson() throws Exception {
    // Arrange
    Note parent = makeMe.aNote().titleConstructor("Parent Note").details("Parent Details").please();
    Note note = makeMe.aNote().under(parent).titleConstructor("Child Note").please();

    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(jsonNode.has(PARENT_URI_AND_TITLE), is(true));
    assertThat(jsonNode.get(PARENT_URI_AND_TITLE).asText(), containsString("Parent Note"));
  }

  @Test
  void shouldIncludeSubjectUriAndTitleButNotParentUriAndTitleWhenNoteIsReified() throws Exception {
    // Arrange
    Note parent = makeMe.aNote().titleConstructor("Parent Note").please();
    Note targetNote =
        makeMe.aNote().titleConstructor("Target Note").details("Target Details").please();
    Note note = makeMe.aLink().between(parent, targetNote).please();

    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(jsonNode.has(PARENT_URI_AND_TITLE), is(false));
    assertThat(jsonNode.has("subjectUriAndTitle"), is(true));
    assertThat(jsonNode.get("subjectUriAndTitle").asText(), containsString("Parent Note"));
  }

  @Test
  void shouldHaveExpectedFieldNamesForNoteWithoutParent() throws Exception {
    // Arrange
    Note note = makeMe.aNote().titleConstructor("Root Note").details("Some details").please();
    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(
        jsonNode::fieldNames, containsInAnyOrder("uri", "title", "details", "relationToFocusNote"));
  }

  @Nested
  class FocusNoteTest {
    @Test
    void shouldIncludeParentUriAndTitleWhenSerializedToJson() throws Exception {
      // Arrange
      Note parent =
          makeMe.aNote().titleConstructor("Parent Note").details("Parent Details").please();
      Note note = makeMe.aNote().under(parent).titleConstructor("Child Note").please();

      FocusNote focusNote = FocusNote.fromNote(note);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(focusNote);

      // Assert
      assertThat(jsonNode.has(PARENT_URI_AND_TITLE), is(true));
      assertThat(jsonNode.get(PARENT_URI_AND_TITLE).asText(), containsString("Parent Note"));
    }

    @Test
    void shouldIncludeSubjectUriAndTitleButNotParentUriAndTitleWhenNoteIsReified()
        throws Exception {
      // Arrange
      Note parent = makeMe.aNote().titleConstructor("Parent Note").please();
      Note targetNote =
          makeMe.aNote().titleConstructor("Target Note").details("Target Details").please();
      Note note = makeMe.aLink().between(parent, targetNote).please();

      FocusNote focusNote = FocusNote.fromNote(note);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(focusNote);

      // Assert
      assertThat(jsonNode.has(PARENT_URI_AND_TITLE), is(false));
      assertThat(jsonNode.has("subjectUriAndTitle"), is(true));
      assertThat(jsonNode.get("subjectUriAndTitle").asText(), containsString("Parent Note"));
    }
  }
}
