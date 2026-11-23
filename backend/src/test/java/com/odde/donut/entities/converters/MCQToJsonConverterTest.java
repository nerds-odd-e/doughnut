package com.odde.donut.entities.converters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.MultipleChoicesQuestion;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MCQToJsonConverterTest {
  private final MCQToJsonConverter converter = new MCQToJsonConverter();

  @Test
  void storesQuestionAsJsonWithIsoReadTime() {
    var question =
        new MultipleChoicesQuestion(
            "Q", List.of("a", "b"), "fp", 7, Timestamp.from(Instant.parse("2026-11-23T05:06:07Z")));

    assertThat(
        converter.convertToDatabaseColumn(question),
        is(
            "{\"questionStem\":\"Q\",\"responseChoices\":[\"a\",\"b\"],\"focusNoteFingerprint\":\"fp\","
                + "\"focusNoteId\":7,\"focusNoteReadAt\":\"2026-11-23T05:06:07.000+00:00\"}"));
  }

  @Test
  void readsLegacyAliasesAndIgnoresUnknownProperties() {
    var question =
        converter.convertToEntityAttribute("{\"stem\":\"Q\",\"choices\":[\"a\"],\"extra\":1}");

    assertThat(question.getQuestionStem(), is("Q"));
    assertThat(question.getResponseChoices(), contains("a"));
  }

  @Test
  void readsAnUnreadablePayloadAsAnEmptyQuestion() {
    assertThat(
        converter.convertToEntityAttribute("not json"), equalTo(new MultipleChoicesQuestion()));
  }
}
