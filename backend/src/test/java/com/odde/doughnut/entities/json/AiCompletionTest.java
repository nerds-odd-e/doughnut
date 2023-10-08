package com.odde.doughnut.entities.json;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.json.AiCompletionParams;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AiCompletionTest {
  @ParameterizedTest
  @CsvSource(
      quoteCharacter = '"',
      textBlock =
          """
    Cow says,        moo.,                Cow says moo.
    "What goes up", ", must come down.",  "What goes up, must come down."
    Корова говорит,  му,                  Корова говорит му
    有烟,             必有火。,             有烟必有火。
  """)
  void prependPreviousIncompleteMessageWithLeadingCharacter(
      String incompleteMessage, String completeSuggestion, String expectedSuggestion) {
    String moreCompleteContent2 = AiCompletionParams.concat(incompleteMessage, completeSuggestion);
    assertEquals(expectedSuggestion, moreCompleteContent2);
  }
}
