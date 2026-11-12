package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ListRecallAnswersWindowMcpEndpointTest extends ListRecallAnswersMcpEndpointTestBase {
  /** The plan's fixed response size. */
  private static final int ATTEMPTS_PER_RESPONSE = 50;

  @Test
  void returnsOnlyTheLearnersOwnAnswersOfTheLastSevenDaysNewestFirst() throws Exception {
    MemoryTracker tracker = trackerOf(personalUser);
    answeredAt(tracker, "Two days ago", hoursAgo(48));
    answeredAt(tracker, "Yesterday", hoursAgo(24));
    answeredAt(tracker, "Eight days ago", hoursAgo(8 * 24));
    answeredAt(trackerOf(makeMe.aUser().please()), "Another learner", hoursAgo(1));
    makeMe.aRecallLogFor(tracker).recordedAt(hoursAgo(2)).please();

    JsonNode result = listRecallAnswers();

    assertThat(
        texts(result.get("attempts"), "/multipleChoice/stem"),
        contains("Yesterday", "Two days ago"));
    assertThat(result.get("from").asText(), equalTo(hoursAgo(7 * 24).toInstant().toString()));
    assertThat(result.get("to").asText(), equalTo(now.toInstant().toString()));
  }

  @Test
  void looksBackTheGivenNumberOfDays() throws Exception {
    MemoryTracker tracker = trackerOf(personalUser);
    answeredAt(tracker, "Ten days ago", hoursAgo(10 * 24));

    assertThat(
        texts(listRecallAnswers(Map.of("days", 14)).get("attempts"), "/multipleChoice/stem"),
        contains("Ten days ago"));
    assertThat(listRecallAnswers().get("attempts").size(), equalTo(0));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void refusesDaysThatAreNotPositive(int days) throws Exception {
    expectRefusal(Map.of("days", days), "A positive number of days is required");
  }

  @Test
  void refusesDaysThatAreNotANumber() throws Exception {
    expectRefusal(Map.of("days", "two weeks"), "/days: string found, number expected");
  }

  @Test
  void continuesAWindowLargerThanOneResponseToItsEndReturningEachAttemptOnce() throws Exception {
    List<String> newestFirst =
        answeredInPairsPerMinute(trackerOf(personalUser), 2 * ATTEMPTS_PER_RESPONSE + 1);

    JsonNode first = listRecallAnswers();
    JsonNode second = listRecallAnswers(Map.of("cursor", first.get("nextCursor").asText()));
    JsonNode last = listRecallAnswers(Map.of("cursor", second.get("nextCursor").asText()));

    assertThat(first.get("attempts").size(), equalTo(ATTEMPTS_PER_RESPONSE));
    assertThat(stemsOf(first, second, last), equalTo(newestFirst));
    assertThat(last.get("nextCursor"), nullValue());
    assertThat(last.get("from"), equalTo(first.get("from")));
    assertThat(last.get("to"), equalTo(first.get("to")));
  }

  @Test
  void anAnswerRecordedWhilePagingNeitherRepeatsNorShiftsAnAttempt() throws Exception {
    MemoryTracker tracker = trackerOf(personalUser);
    List<String> newestFirst = answeredInPairsPerMinute(tracker, ATTEMPTS_PER_RESPONSE + 2);
    JsonNode first = listRecallAnswers();
    Timestamp later = TimestampOperations.addHoursToTimestamp(now, 1);
    testabilitySettings.timeTravelTo(later);
    answeredAt(tracker, "While paging", later);

    JsonNode last = listRecallAnswers(Map.of("cursor", first.get("nextCursor").asText()));

    assertThat(stemsOf(first, last), equalTo(newestFirst));
    assertThat(last.get("nextCursor"), nullValue());
  }

  @Test
  void refusesACursorThatCannotBeRead() throws Exception {
    expectRefusal(
        Map.of("cursor", "not a cursor"),
        "The cursor cannot be read. Start again without a cursor");
  }

  /** Answers, oldest created first, two per minute so a page can end between equal times. */
  private List<String> answeredInPairsPerMinute(MemoryTracker tracker, int count) {
    List<String> stems = IntStream.range(0, count).mapToObj(i -> "Attempt " + i).toList();
    IntStream.range(0, count)
        .forEach(i ->
            answeredAt(
                tracker,
                stems.get(i),
                Timestamp.from(now.toInstant().minusSeconds(60L * ((count - i) / 2)))));
    return stems.reversed();
  }

  private static List<String> stemsOf(JsonNode... responses) {
    List<String> stems = new ArrayList<>();
    for (JsonNode response : responses) {
      stems.addAll(texts(response.get("attempts"), "/multipleChoice/stem"));
    }
    return stems;
  }

  private void expectRefusal(Map<String, Object> arguments, String message) throws Exception {
    mockMvc
        .perform(authorized(mcpPost(callBody(arguments))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.isError").value(true))
        .andExpect(jsonPath("$.result.content[0].text").value(containsString(message)));
  }

  private void answeredAt(MemoryTracker tracker, String stem, Timestamp answeredAt) {
    makeMe
        .aRecallPrompt()
        .withMcq(makeMe.anMcq().forNote(note).stem(stem).inMemoryPlease())
        .forMemoryTracker(tracker)
        .answerChoiceIndex(0)
        .answerTimestamp(answeredAt)
        .recorded()
        .please();
  }
}
