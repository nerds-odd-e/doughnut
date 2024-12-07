package com.odde.doughnut.services.graphRAG;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.Note;
import org.junit.jupiter.api.Test;

class CharacterBasedTokenCountingStrategyTest {
  @Test
  void shouldEstimateTokensBasedOnCharacterCount() {
    CharacterBasedTokenCountingStrategy strategy = new CharacterBasedTokenCountingStrategy();
    Note note = new Note();
    note.setDetails("1234567890"); // 10 characters
    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Parent);

    assertEquals(3, strategy.estimateTokens(bareNote)); // 10/4 + 1 = 3
  }

  @Test
  void shouldReturnOneForNullDetails() {
    CharacterBasedTokenCountingStrategy strategy = new CharacterBasedTokenCountingStrategy();
    Note note = new Note();
    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Parent);

    assertEquals(1, strategy.estimateTokens(bareNote));
  }
}
