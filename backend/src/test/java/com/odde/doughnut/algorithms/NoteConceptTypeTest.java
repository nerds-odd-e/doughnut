package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

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
  void leavesClosedLeadingFenceUnchanged() {
    String alreadyFenced = "---\nparent: \"[[Course intro]]\"\n---\n";
    assertThat(NoteConceptType.ensureOrdinaryNoteType(alreadyFenced), equalTo(alreadyFenced));
  }
}
