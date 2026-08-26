package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NoteConceptTypeTest {

  private static final String ORDINARY_NOTE_FENCE = "---\ntype: Note\n---\n";

  @ParameterizedTest
  @NullAndEmptySource
  void wrapsNullOrEmptyWithOrdinaryNoteType(String content) {
    assertThat(NoteConceptType.ensureStoredType(content), equalTo(ORDINARY_NOTE_FENCE));
  }

  @Test
  void wrapsUnclosedFenceAsNoFence() {
    String unclosed = "---\naliases:\n  - x\nbody";
    assertThat(NoteConceptType.ensureStoredType(unclosed), equalTo(ORDINARY_NOTE_FENCE + unclosed));
  }

  @Test
  void insertsOrdinaryNoteTypeAsFirstKeyKeepingFenceAndBodyVerbatim() {
    String content =
        """
        ---
        parent: "[[Course intro]]"
        aliases:
          - x
        # keep me
        ---
        body line
        """;
    assertThat(
        NoteConceptType.ensureStoredType(content),
        equalTo(
            """
            ---
            type: Note
            parent: "[[Course intro]]"
            aliases:
              - x
            # keep me
            ---
            body line
            """));
  }

  @ParameterizedTest
  @ValueSource(strings = {"type:", "type: ", "type: \"\"", "type: ''"})
  void treatsBlankTypeAsMissingAndInsertsFirst(String typeLine) {
    String content = "---\nparent: \"[[x]]\"\n" + typeLine + "\naliases:\n  - y\n---\n";
    assertThat(
        NoteConceptType.ensureStoredType(content),
        equalTo("---\ntype: Note\nparent: \"[[x]]\"\naliases:\n  - y\n---\n"));
  }

  @ParameterizedTest
  @CsvSource(
      textBlock =
          """
          note,                 Note
          NOTE,                 Note
          Note,                 Note
          '"note"',             Note
          '"NOTE"',             Note
          '"Note"',             Note
          '''note''',           Note
          relationship,         Relationship
          RELATIONSHIP,         Relationship
          Relationship,         Relationship
          '"relationship"',     Relationship
          '"RELATIONSHIP"',     Relationship
          '''relationship''',   Relationship
          """)
  void canonicalizesStoredTypeSpellingInPlace(String givenType, String canonicalType) {
    String content = "---\ntype: " + givenType + "\nparent: x\n---\nbody";
    assertThat(
        NoteConceptType.ensureStoredType(content),
        equalTo("---\ntype: " + canonicalType + "\nparent: x\n---\nbody"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Attested Computation", "\"Attested Computation\""})
  void leavesUnknownNonEmptyTypeUnchanged(String type) {
    String content = "---\ntype: " + type + "\nparent: x\n---\nbody";
    assertThat(NoteConceptType.ensureStoredType(content), equalTo(content));
  }
}
