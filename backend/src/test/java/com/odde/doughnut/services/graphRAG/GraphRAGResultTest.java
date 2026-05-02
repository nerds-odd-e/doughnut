package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
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
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  @Test
  void shouldNotSerializeParentOnBareNoteChildUsesTitleOnly() throws Exception {
    // Arrange
    Note parent = makeMe.aNote().title("Parent Note").details("Parent Details").please();
    Note note = makeMe.aNote().under(parent).title("Child Note").please();

    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(jsonNode.has("parent"), is(false));
    assertThat(jsonNode.get("title").asText(), containsString("Child Note"));
    assertThat(jsonNode.has("uri"), is(true));
  }

  @Nested
  class RelatedNoteTest {
    private Note parent;
    private Note targetNote;
    private Note note;
    private JsonNode jsonNode;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().title("Parent Note").please();
      targetNote = makeMe.aNote().title("Target Note").details("Target Details").please();
      note = makeMe.aRelation().between(parent, targetNote).please();

      BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);
      jsonNode = objectMapper.valueToTree(bareNote);
    }

    @Test
    void shouldOmitSubjectAndParentWhenNoteIsRelated() {
      assertThat(jsonNode.has("parent"), is(false));
      assertThat(jsonNode.has("subject"), is(false));
      assertThat(jsonNode.get("details").asText(), containsString("Parent Note"));
    }

    @Test
    void shouldIncludeTitleAndOmitRelationTypeWhenNoteIsRelated() {
      assertThat(jsonNode.has("relation_type"), is(false));
      assertThat(jsonNode.has("title"), is(true));
      assertThat(jsonNode.get("title").asText(), equalTo(note.getTitle()));
    }

    @Test
    void shouldHaveCorrectPropertyOrderForRelatedNote() throws Exception {
      // Act
      String jsonString = objectMapper.writeValueAsString(jsonNode);

      // Assert property order
      assertThat(jsonString, startsWith("{\"uri\":"));
      assertThat(jsonString.indexOf("\"title\":"), is(greaterThan(jsonString.indexOf("\"uri\":"))));
      assertThat(jsonString, not(containsString("\"subject\"")));
      assertThat(jsonString, not(containsString("\"target\"")));
      assertThat(jsonString, not(containsString("\"relation_type\"")));
      assertThat(
          jsonString.indexOf("\"relationToFocusNote\":"),
          is(greaterThan(jsonString.indexOf("\"title\":"))));
    }
  }

  @Test
  void shouldHaveExpectedFieldNamesForNoteWithoutParent() throws Exception {
    // Arrange
    Note note = makeMe.aNote().title("Root Note").details("Some details").please();
    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(
        jsonNode::fieldNames,
        containsInAnyOrder(
            "uri",
            "title",
            "details",
            "relationToFocusNote",
            "linkFromFocus",
            "linkHop2",
            "createdAt"));
    // detailsTruncated should not be present when details are not truncated
    assertThat(jsonNode.has("detailsTruncated"), is(false));
  }

  @Test
  void shouldIncludeCreatedAtWhenSerializedToJson() throws Exception {
    // Arrange
    Timestamp createdAt = new Timestamp(System.currentTimeMillis() - 86400000); // 1 day ago
    Timestamp updatedAt = new Timestamp(System.currentTimeMillis());
    Note note =
        makeMe
            .aNote()
            .title("Test Note")
            .details("Test Details")
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .please();
    makeMe.refresh(note);

    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

    // Act
    JsonNode jsonNode = objectMapper.valueToTree(bareNote);

    // Assert
    assertThat(jsonNode.has("createdAt"), is(true));
    assertThat(jsonNode.has("updatedAt"), is(false));
    // Timestamps are serialized as ISO strings when WRITE_DATES_AS_TIMESTAMPS is disabled
    assertThat(jsonNode.get("createdAt").asText(), is(not(emptyString())));
  }

  @Nested
  class FocusNoteTest {
    @Test
    void shouldExposeParentPlacementViaContextualPathNotParentField() throws Exception {
      // Arrange
      Note parent = makeMe.aNote().title("Parent Note").details("Parent Details").please();
      Note note = makeMe.aNote().under(parent).title("Child Note").please();

      FocusNote focusNote = FocusNote.fromNote(note);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(focusNote);
      String jsonString = objectMapper.writeValueAsString(focusNote);

      // Assert
      assertThat(jsonNode.has("parent"), is(false));
      assertThat(jsonNode.has("contextualPath"), is(true));
      assertThat(jsonNode.get("contextualPath").toString(), containsString(parent.getUri()));

      // Assert property order
      assertThat(jsonString, startsWith("{\"uri\":"));
      assertThat(jsonString.indexOf("\"title\":"), is(greaterThan(jsonString.indexOf("\"uri\":"))));
      assertThat(
          jsonString.indexOf("\"details\":"), is(greaterThan(jsonString.indexOf("\"title\":"))));
    }

    @Test
    void shouldIncludeCreatedAtWhenSerializedToJson() throws Exception {
      // Arrange
      Timestamp createdAt = new Timestamp(System.currentTimeMillis() - 86400000); // 1 day ago
      Timestamp updatedAt = new Timestamp(System.currentTimeMillis());
      Note note =
          makeMe
              .aNote()
              .title("Focus Note")
              .details("Focus Details")
              .createdAt(createdAt)
              .updatedAt(updatedAt)
              .please();
      makeMe.refresh(note);

      FocusNote focusNote = FocusNote.fromNote(note);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(focusNote);

      // Assert
      assertThat(jsonNode.has("createdAt"), is(true));
      assertThat(jsonNode.has("updatedAt"), is(false));
      // Timestamps are serialized as ISO strings when WRITE_DATES_AS_TIMESTAMPS is disabled
      assertThat(jsonNode.get("createdAt").asText(), is(not(emptyString())));
    }
  }

  @Nested
  class DetailsTruncationTest {
    @Test
    void shouldNotIncludeDetailsTruncatedWhenDetailsAreNotTruncated() throws Exception {
      // Arrange - details shorter than truncation limit
      Note note = makeMe.aNote().title("Short Note").details("Short details").please();
      BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(bareNote);

      // Assert
      assertThat(jsonNode.has("detailsTruncated"), is(false));
      assertThat(jsonNode.get("details").asText(), is("Short details"));
    }

    @Test
    void shouldIncludeDetailsTruncatedAsTrueWhenDetailsAreTruncated() throws Exception {
      // Arrange - create details longer than truncation limit (500 bytes)
      String longDetails = "x".repeat(600); // 600 characters should exceed 500 bytes
      Note note = makeMe.aNote().title("Long Note").details(longDetails).please();
      BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Child);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(bareNote);

      // Assert
      assertThat(jsonNode.has("detailsTruncated"), is(true));
      assertThat(jsonNode.get("detailsTruncated").asBoolean(), is(true));
      assertThat(jsonNode.get("details").asText(), endsWith("..."));
    }

    @Test
    void shouldNotIncludeDetailsTruncatedForFocusNote() throws Exception {
      // Arrange
      Note note = makeMe.aNote().title("Focus Note").details("Some details").please();
      FocusNote focusNote = FocusNote.fromNote(note);

      // Act
      JsonNode jsonNode = objectMapper.valueToTree(focusNote);

      // Assert - FocusNote uses fromNoteWithoutTruncate internally, so detailsTruncated should be
      // null
      assertThat(jsonNode.has("detailsTruncated"), is(false));
    }
  }
}
