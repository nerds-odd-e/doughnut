package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GeneratedMcqTest {

  @Test
  void acceptsOneCorrectAnswerAndThreeDistinctDistractors() {
    GeneratedMcq generatedMcq =
        generatedMcq(
            "What is the capital of France?", "Paris", List.of("London", "Rome", "Berlin"));

    assertThat(generatedMcq.isValid(), is(true));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidGeneratedMcqs")
  void rejectsInvalidSemanticAnswerSets(String scenario, GeneratedMcq generatedMcq) {
    assertThat(generatedMcq.isValid(), is(false));
  }

  static Stream<Arguments> invalidGeneratedMcqs() {
    return Stream.of(
        arguments("blank stem", generatedMcq(" ", "correct", List.of("one", "two", "three"))),
        arguments(
            "blank correct answer", generatedMcq("stem", " ", List.of("one", "two", "three"))),
        arguments("missing distractors", generatedMcq("stem", "correct", null)),
        arguments("too few distractors", generatedMcq("stem", "correct", List.of("one", "two"))),
        arguments(
            "too many distractors",
            generatedMcq("stem", "correct", List.of("one", "two", "three", "four"))),
        arguments(
            "blank distractor", generatedMcq("stem", "correct", List.of("one", " \t", "three"))),
        arguments(
            "null distractor",
            generatedMcq("stem", "correct", Arrays.asList("one", null, "three"))),
        arguments(
            "repeated distractor", generatedMcq("stem", "correct", List.of("one", "two", "one"))),
        arguments(
            "repeated distractor after stripping whitespace",
            generatedMcq("stem", "correct", List.of(" one", "two", "one "))),
        arguments(
            "distractor duplicates correct answer after stripping whitespace",
            generatedMcq("stem", " correct ", List.of("one", "correct", "three"))));
  }

  private static GeneratedMcq generatedMcq(
      String questionStem, String correctAnswer, List<String> distractors) {
    return new GeneratedMcq(questionStem, correctAnswer, distractors, null, null);
  }
}
