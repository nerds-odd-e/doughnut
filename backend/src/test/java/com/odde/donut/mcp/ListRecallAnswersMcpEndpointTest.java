package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class ListRecallAnswersMcpEndpointTest extends ListRecallAnswersMcpEndpointTestBase {
  @Test
  void returnsTheFullRecordedMultipleChoiceAttempt() throws Exception {
    var mcq =
        makeMe
            .anMcq()
            .stem("Who reviews code?")
            .choices("Peers", "Nobody", "The compiler")
            .correctAnswerIndex(0)
            .validationRationale("Peers review code.")
            .please();
    Timestamp yesterday = hoursAgo(24);
    makeMe
        .aRecallPrompt()
        .withMcq(mcq)
        .forMemoryTracker(trackerOf(personalUser))
        .answerChoiceIndex(1)
        .answerTimestamp(yesterday)
        .recorded()
        .please();
    makeMe.entityPersister.flushAndClear();

    JsonNode attempt = listRecallAnswers().get("attempts").get(0);

    assertThat(attempt.get("answeredAt").asText(), equalTo(yesterday.toInstant().toString()));
    assertThat(attempt.get("correct"), equalTo(BooleanNode.FALSE));
    assertThat(attempt.at("/note/title").asText(), equalTo("Code Review"));
    assertThat(attempt.at("/note/portablePath").asText(), equalTo("Engineering:Code Review"));
    JsonNode question = attempt.get("multipleChoice");
    assertThat(question.get("stem").asText(), equalTo("Who reviews code?"));
    assertThat(texts(question.get("choices")), contains("Peers", "Nobody", "The compiler"));
    assertThat(question.get("selectedChoice").asText(), equalTo("Nobody"));
    assertThat(question.get("expectedAnswer").asText(), equalTo("Peers"));
    assertThat(question.get("validationRationale").asText(), equalTo("Peers review code."));
    assertThat(attempt.has("excludedAsAmbiguous"), equalTo(false));
  }

  @Test
  void returnsAnAmbiguityExcludedAttemptAsRecordedWithItsReason() throws Exception {
    makeMe
        .aRecallPrompt()
        .forMemoryTracker(trackerOf(personalUser))
        .answerChoiceIndex(1)
        .answerTimestamp(hoursAgo(24))
        .recorded()
        .excludedAsAmbiguous("both choices fit")
        .please();
    makeMe.entityPersister.flushAndClear();

    JsonNode attempt = listRecallAnswers().get("attempts").get(0);

    assertThat(attempt.get("correct"), equalTo(BooleanNode.FALSE));
    assertThat(attempt.at("/excludedAsAmbiguous/reason").asText(), equalTo("both choices fit"));
    assertThat(
        attempt.at("/note/graph/focusNote/portablePath").asText(),
        equalTo("Engineering:Code Review"));
  }

  @Test
  void returnsTheSpellingAttemptWithItsTypedAnswerAndPrompt() throws Exception {
    noteLastUpdatedAt(hoursAgo(48));
    answeredSpellingIncorrectly("Code Reveiw", hoursAgo(24));

    JsonNode attempt = listRecallAnswers().get("attempts").get(0);

    assertThat(attempt.get("correct"), equalTo(BooleanNode.FALSE));
    JsonNode spelling = attempt.get("spelling");
    assertThat(spelling.get("typedAnswer").asText(), equalTo("Code Reveiw"));
    assertThat(
        spelling.get("prompt").asText(),
        equalTo(
            "A <mark title='Hidden text that is matching the answer'>[...]</mark>"
                + " catches bugs early."));
    assertThat(spelling.has("promptUnavailable"), equalTo(false));
    assertThat(attempt.has("outcome"), equalTo(false));
    assertThat(attempt.has("excludedAsAmbiguous"), equalTo(false));
  }

  @Test
  void reportsTheSpellingPromptUnavailableWhenTheNoteWasEditedAfterTheAnswer() throws Exception {
    noteLastUpdatedAt(hoursAgo(1));
    answeredSpellingIncorrectly("Code Reveiw", hoursAgo(24));

    JsonNode attempt = listRecallAnswers().get("attempts").get(0);

    assertThat(attempt.get("correct"), equalTo(BooleanNode.FALSE));
    JsonNode spelling = attempt.get("spelling");
    assertThat(spelling.get("typedAnswer").asText(), equalTo("Code Reveiw"));
    assertThat(spelling.has("prompt"), equalTo(false));
    assertThat(spelling.get("promptUnavailable").asText(), containsString("edited after"));
  }

  @Test
  void returnsAnOverlapAsACorrectAttemptNamingTheOutcome() throws Exception {
    makeMe
        .aRecallPrompt()
        .forMemoryTracker(trackerOf(personalUser))
        .answerSpelling("Review")
        .overlap()
        .answerTimestamp(hoursAgo(24))
        .please();
    makeMe.entityPersister.flushAndClear();

    JsonNode attempt = listRecallAnswers().get("attempts").get(0);

    assertThat(attempt.get("correct"), equalTo(BooleanNode.TRUE));
    assertThat(attempt.get("outcome").asText(), equalTo("OVERLAP"));
    assertThat(attempt.at("/spelling/typedAnswer").asText(), equalTo("Review"));
    assertTitleAndPathOnly(attempt.get("note"));
  }

  @Test
  void returnsAnAccidentalMatchAsOneIncorrectAttemptNamingTheOutcome() throws Exception {
    Note otherNote = makeMe.aNote("Pair Programming").underSameNotebookAs(note).please();
    MemoryTracker otherTracker = makeMe.aMemoryTrackerFor(otherNote).by(personalUser).please();
    var prompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(trackerOf(personalUser))
            .answerSpelling("Pair Programming")
            .accidentalMatch()
            .answerTimestamp(hoursAgo(24))
            .recorded()
            .please();
    makeMe
        .aRecallLogFor(otherTracker)
        .confusion()
        .answer(prompt.getAnswer())
        .recordedAt(hoursAgo(24))
        .please();
    makeMe.entityPersister.flushAndClear();

    JsonNode attempts = listRecallAnswers().get("attempts");

    assertThat(attempts.size(), equalTo(1));
    JsonNode attempt = attempts.get(0);
    assertThat(attempt.get("correct"), equalTo(BooleanNode.FALSE));
    assertThat(attempt.get("outcome").asText(), equalTo("ACCIDENTAL_MATCH"));
    assertThat(attempt.at("/note/title").asText(), equalTo("Code Review"));
    assertThat(attempt.at("/spelling/typedAnswer").asText(), equalTo("Pair Programming"));
  }

  private void noteLastUpdatedAt(Timestamp updatedAt) {
    makeMe.theNote(note).content("A code review catches bugs early.").updatedAt(updatedAt).please();
  }

  private void answeredSpellingIncorrectly(String typed, Timestamp answeredAt) {
    makeMe
        .aRecallPrompt()
        .spelling()
        .forMemoryTracker(trackerOf(personalUser))
        .answerSpellingIncorrectly(typed)
        .answerTimestamp(answeredAt)
        .recorded()
        .please();
    makeMe.entityPersister.flushAndClear();
  }
}
