package com.odde.doughnut.services.graphRAG;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CharacterBasedTokenCountingStrategyTest {
  private CharacterBasedTokenCountingStrategy strategy;
  private Note note;

  @BeforeEach
  void setUp() {
    strategy = new CharacterBasedTokenCountingStrategy();
    note = mock(Note.class);
  }

  @Test
  void shouldCountTitleAndDetailsCharacters() {
    when(note.getTopicConstructor()).thenReturn("Title"); // 5 chars
    when(note.getDetails()).thenReturn("Details"); // 7 chars

    // (5 + 7) / 3.75 = 3.2 -> 4 tokens
    assertThat(strategy.estimateTokens(note), equalTo(4));
  }

  @Test
  void shouldHandleNullDetails() {
    when(note.getTopicConstructor()).thenReturn("Title"); // 5 chars
    when(note.getDetails()).thenReturn(null);

    // 5 / 3.75 = 1.33 -> 2 tokens
    assertThat(strategy.estimateTokens(note), equalTo(2));
  }

  @Test
  void shouldTruncateLongDetails() {
    when(note.getTopicConstructor()).thenReturn("Title");
    when(note.getDetails()).thenReturn("a".repeat(1000));

    // (5 + 500) / 3.75 = 134.67 -> 135 tokens
    assertThat(strategy.estimateTokens(note), equalTo(135));
  }
}
