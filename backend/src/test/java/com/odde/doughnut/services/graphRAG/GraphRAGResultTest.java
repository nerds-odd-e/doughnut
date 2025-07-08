package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
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
class GraphRAGResultTest {
  @Autowired private MakeMe makeMe;
  private final ObjectMapper objectMapper =
      new com.odde.doughnut.configs.ObjectMapperConfig().objectMapper();
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

  @Nested
  class ReifiedNoteTest {
    private Note parent;
    private Note targetNote;
    private Note note;
    private JsonNode jsonNode;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().titleConstructor("Parent Note").please();
      targetNote =
          makeMe.aNote().titleConstructor("Target Note").details("Target Details").please();
      note = makeMe.aReification().between(parent, targetNote).please();

      BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);
      jsonNode = objectMapper.valueToTree(bareNote);
    }

    @Test
    void shouldIncludeSubjectUriAndTitleButNotParentUriAndTitleWhenNoteIsReified() {
      // Assert
      assertThat(jsonNode.has(PARENT_URI_AND_TITLE), is(false));
      assertThat(jsonNode.has("subjectUriAndTitle"), is(true));
      assertThat(jsonNode.get("subjectUriAndTitle").asText(), containsString("Parent Note"));
    }

    @Test
    void shouldHavePredicateFieldInsteadOfTitleWhenNoteIsReified() {
      assertThat(jsonNode.has("title"), is(false));
      assertThat(jsonNode.has("predicate"), is(true));
      assertThat(jsonNode.get("predicate").asText(), is(":a specialization of"));
    }

    @Test
    void shouldHaveCorrectPropertyOrderForReifiedNote() throws Exception {
      // Act
      String jsonString = objectMapper.writeValueAsString(jsonNode);

      // Assert property order
      assertThat(jsonString, startsWith("{\"uri\":"));
      assertThat(
          jsonString.indexOf("\"subjectUriAndTitle\":"),
          is(greaterThan(jsonString.indexOf("\"uri\":"))));
      assertThat(
          jsonString.indexOf("\"predicate\":"),
          is(greaterThan(jsonString.indexOf("\"subjectUriAndTitle\":"))));
      assertThat(
          jsonString.indexOf("\"objectUriAndTitle\":"),
          is(greaterThan(jsonString.indexOf("\"predicate\":"))));
    }
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
        jsonNode::fieldNames,
        containsInAnyOrder("uri", "title", "details", "relationToFocusNote", "createdAt"));
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
      String jsonString = objectMapper.writeValueAsString(focusNote);

      // Assert
      assertThat(jsonNode.has(PARENT_URI_AND_TITLE), is(true));
      assertThat(jsonNode.get(PARENT_URI_AND_TITLE).asText(), containsString("Parent Note"));

      // Assert property order
      assertThat(jsonString, startsWith("{\"uri\":"));
      assertThat(jsonString.indexOf("\"title\":"), is(greaterThan(jsonString.indexOf("\"uri\":"))));
      assertThat(
          jsonString.indexOf("\"details\":"), is(greaterThan(jsonString.indexOf("\"title\":"))));
    }
  }
}
