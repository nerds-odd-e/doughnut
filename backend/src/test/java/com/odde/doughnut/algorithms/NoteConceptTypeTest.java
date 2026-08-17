package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NoteConceptTypeTest {

  private static final String ORDINARY_NOTE_FENCE = "---\ntype: Note\n---\n";

  @ParameterizedTest
  @NullAndEmptySource
  void wrapsNullOrEmptyWithOrdinaryNoteType(String content) {
    assertThat(NoteConceptType.ensureOrdinaryNoteType(content), equalTo(ORDINARY_NOTE_FENCE));
  }

  @Test
  void wrapsUnclosedFenceAsNoFence() {
    String unclosed = "---\naliases:\n  - x\nbody";
    assertThat(
        NoteConceptType.ensureOrdinaryNoteType(unclosed), equalTo(ORDINARY_NOTE_FENCE + unclosed));
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
        NoteConceptType.ensureOrdinaryNoteType(content),
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
        NoteConceptType.ensureOrdinaryNoteType(content),
        equalTo("---\ntype: Note\nparent: \"[[x]]\"\naliases:\n  - y\n---\n"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"relationship", "note", "Note", "Attested Computation"})
  void leavesExistingNonEmptyTypeUnchanged(String type) {
    String content = "---\ntype: " + type + "\nparent: x\n---\nbody";
    assertThat(NoteConceptType.ensureOrdinaryNoteType(content), equalTo(content));
  }
}
