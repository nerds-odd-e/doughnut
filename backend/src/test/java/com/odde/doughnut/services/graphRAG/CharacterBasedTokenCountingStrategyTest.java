package com.odde.doughnut.services.graphRAG;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Note;
import org.junit.jupiter.api.Test;

class CharacterBasedTokenCountingStrategyTest {
  @Test
  void shouldEstimateTokensBasedOnJsonLength() {
    CharacterBasedTokenCountingStrategy strategy = new CharacterBasedTokenCountingStrategy();
    Note note = new Note();
    note.setDetails("1234567890"); // 10 characters
    BareNote bareNote = BareNote.fromNote(note, RelationshipToFocusNote.Parent);

    int tokens = strategy.estimateTokens(bareNote);
    assertTrue(tokens > 0, "Should estimate some tokens");
  }

  @Test
  void shouldEstimateMoreTokensForLongerContent() {
    CharacterBasedTokenCountingStrategy strategy = new CharacterBasedTokenCountingStrategy();
    Note note1 = new Note();
    note1.setDetails("short");
    BareNote bareNote1 = BareNote.fromNote(note1, RelationshipToFocusNote.Parent);

    Note note2 = new Note();
    note2.setDetails("this is a much longer piece of text that should result in more tokens");
    BareNote bareNote2 = BareNote.fromNote(note2, RelationshipToFocusNote.Parent);

    int tokens1 = strategy.estimateTokens(bareNote1);
    int tokens2 = strategy.estimateTokens(bareNote2);

    assertTrue(tokens2 > tokens1, "Longer content should result in more tokens");
  }
}
