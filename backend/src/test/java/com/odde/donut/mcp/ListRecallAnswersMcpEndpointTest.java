package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BooleanNode;

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

    assertThat(attempt.get("answeredAt").asString(), equalTo(yesterday.toInstant().toString()));
    assertThat(attempt.get("correct"), equalTo(BooleanNode.FALSE));
    assertThat(attempt.at("/note/title").asString(), equalTo("Code Review"));
    assertThat(attempt.at("/note/portablePath").asString(), equalTo("Engineering:Code Review"));
    JsonNode question = attempt.get("multipleChoice");
    assertThat(question.get("stem").asString(), equalTo("Who reviews code?"));
    assertThat(texts(question.get("choices")), contains("Peers", "Nobody", "The compiler"));
    assertThat(question.get("selectedChoice").asString(), equalTo("Nobody"));
    assertThat(question.get("expectedAnswer").asString(), equalTo("Peers"));
    assertThat(question.get("validationRationale").asString(), equalTo("Peers review code."));
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
    assertThat(attempt.at("/excludedAsAmbiguous/reason").asString(), equalTo("both choices fit"));
    assertThat(
        attempt.at("/note/graph/focusNote/portablePath").asString(),
        equalTo("Engineering:Code Review"));
  }

  @Test
  void returnsTheSpellingAttemptWithItsTypedAnswerAndPrompt() throws Exception {
    noteLastUpdatedAt(hoursAgo(48));
    answeredSpellingIncorrectly("Code Reveiw", hoursAgo(24));

    JsonNode attempt = listRecallAnswers().get("attempts").get(0);

    assertThat(attempt.get("correct"), equalTo(BooleanNode.FALSE));
    JsonNode spelling = attempt.get("spelling");
    assertThat(spelling.get("typedAnswer").asString(), equalTo("Code Reveiw"));
    assertThat(
        spelling.get("prompt").asString(),
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
    assertThat(spelling.get("typedAnswer").asString(), equalTo("Code Reveiw"));
    assertThat(spelling.has("prompt"), equalTo(false));
    assertThat(spelling.get("promptUnavailable").asString(), containsString("edited after"));
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
    assertThat(attempt.get("outcome").asString(), equalTo("OVERLAP"));
    assertThat(attempt.at("/spelling/typedAnswer").asString(), equalTo("Review"));
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
    assertThat(attempt.get("outcome").asString(), equalTo("ACCIDENTAL_MATCH"));
    assertThat(attempt.at("/note/title").asString(), equalTo("Code Review"));
    assertThat(attempt.at("/spelling/typedAnswer").asString(), equalTo("Pair Programming"));
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
