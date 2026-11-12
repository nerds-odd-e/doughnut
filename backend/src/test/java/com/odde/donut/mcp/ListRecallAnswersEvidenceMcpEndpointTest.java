package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.donut.services.ApproximateUtf8TokenBudget;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

/** Which source note evidence an attempt carries, chosen by recorded correctness and type. */
class ListRecallAnswersEvidenceMcpEndpointTest extends ListRecallAnswersMcpEndpointTestBase {
  @Test
  void aCorrectMultipleChoiceAttemptCarriesTitleAndPathOnly() throws Exception {
    answeredMultipleChoice(0, hoursAgo(24));

    JsonNode attempt = listRecallAnswers().get("attempts").get(0);

    assertThat(attempt.at("/multipleChoice/stem").asText(), equalTo("Who reviews code?"));
    assertTitleAndPathOnly(attempt.get("note"));
  }

  @Test
  void aCorrectSpellingAttemptCarriesTitleAndPathOnly() throws Exception {
    makeMe
        .aRecallPrompt()
        .spelling()
        .forMemoryTracker(trackerOf(personalUser))
        .answerSpelling("Code Review")
        .answerTimestamp(hoursAgo(24))
        .recorded()
        .please();
    makeMe.entityPersister.flushAndClear();

    assertTitleAndPathOnly(listRecallAnswers().at("/attempts/0/note"));
  }

  @Test
  void anIncorrectSpellingAttemptAddsTheNotesFullCurrentContent() throws Exception {
    makeMe.theNote(note).content("A code review catches bugs early.").please();
    makeMe
        .aRecallPrompt()
        .spelling()
        .forMemoryTracker(trackerOf(personalUser))
        .answerSpellingIncorrectly("Code Reveiw")
        .answerTimestamp(hoursAgo(24))
        .recorded()
        .please();
    makeMe.entityPersister.flushAndClear();

    JsonNode sourceNote = listRecallAnswers().at("/attempts/0/note");

    assertThat(sourceNote.get("title").asText(), equalTo("Code Review"));
    assertThat(sourceNote.get("portablePath").asText(), equalTo("Engineering:Code Review"));
    assertThat(sourceNote.get("content").asText(), equalTo("A code review catches bugs early."));
    assertThat(sourceNote.has("graph"), equalTo(false));
  }

  @Test
  void anIncorrectMultipleChoiceAttemptAddsTheBoundedGraphAddressedByPortablePath()
      throws Exception {
    makeMe.theNote(note).content("Often done with [[Pair Programming]].").please();
    makeMe.aNote("Pair Programming").underSameNotebookAs(note).please();
    answeredMultipleChoice(1, hoursAgo(10 * 24));

    JsonNode sourceNote = listRecallAnswers(Map.of("days", 14)).at("/attempts/0/note");

    assertThat(sourceNote.get("title").asText(), equalTo("Code Review"));
    assertThat(sourceNote.get("portablePath").asText(), equalTo("Engineering:Code Review"));
    assertThat(sourceNote.has("content"), equalTo(false));
    JsonNode graph = sourceNote.get("graph");
    assertThat(graph.at("/focusNote/portablePath").asText(), equalTo("Engineering:Code Review"));
    assertThat(graph.get("focusNote").has("id"), equalTo(false));
    assertThat(
        texts(graph.get("relatedNotes"), "/portablePath"), hasItem("Engineering:Pair Programming"));
  }

  @Test
  void theBoundedGraphStaysWithinItsBudget() throws Exception {
    makeMe.theNote(note).content("See [[One]], [[Two]] and [[Three]].").please();
    for (String title : List.of("One", "Two", "Three")) {
      makeMe.aNote(title).underSameNotebookAs(note).content("word ".repeat(800)).please();
    }
    answeredMultipleChoice(1, hoursAgo(24));

    JsonNode graph = listRecallAnswers().at("/attempts/0/note/graph");

    assertThat(graph.get("relatedNotes").size(), greaterThan(0));
    int tokens =
        ApproximateUtf8TokenBudget.estimateApproxTokens(graph.at("/focusNote/content").asText())
            + StreamSupport.stream(graph.get("relatedNotes").spliterator(), false)
                .mapToInt(related ->
                    ApproximateUtf8TokenBudget.estimateApproxTokens(
                        related.get("content").asText()))
                .sum();
    assertThat(tokens, lessThanOrEqualTo(1000));
  }

  private void answeredMultipleChoice(int choiceIndex, Timestamp answeredAt) {
    makeMe
        .aRecallPrompt()
        .withMcq(
            makeMe
                .anMcq()
                .forNote(note)
                .stem("Who reviews code?")
                .choices("Peers", "Nobody")
                .correctAnswerIndex(0)
                .inMemoryPlease())
        .forMemoryTracker(trackerOf(personalUser))
        .answerChoiceIndex(choiceIndex)
        .answerTimestamp(answeredAt)
        .recorded()
        .please();
    makeMe.entityPersister.flushAndClear();
  }
}
