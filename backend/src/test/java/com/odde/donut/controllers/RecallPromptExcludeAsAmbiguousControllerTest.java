package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.AmbiguousExclusionDTO;
import com.odde.donut.entities.Answer;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.QuestionAnswerException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecallPromptExcludeAsAmbiguousControllerTest extends RecallPromptControllerTestBase {
  Note note;
  RecallPrompt recallPrompt;
  Timestamp answeredAt;
  Timestamp excludedAt;

  @BeforeEach
  void setup() throws UnexpectedNoAccessRightException {
    answeredAt = makeMe.aTimestamp().of(1, 8).please();
    excludedAt = makeMe.aTimestamp().of(2, 8).please();
    note = ownedNote();
    recallPrompt = mcqPrompt(ownedTracker(note), note);
    testabilitySettings.timeTravelTo(answeredAt);
    controller.answer(recallPrompt, choiceAnswer(1));
    testabilitySettings.timeTravelTo(excludedAt);
  }

  @Test
  void recordsTheExclusionAndKeepsTheOriginalMark() throws UnexpectedNoAccessRightException {
    Answer answer =
        controller.excludeAsAmbiguous(recallPrompt, exclusion("both tenses fit")).getAnswer();

    makeMe.refresh(answer);
    assertThat(answer.getExcludedAt(), equalTo(excludedAt));
    assertThat(answer.getExclusionReason(), equalTo("both tenses fit"));
    assertThat(answer.getCorrect(), is(false));
  }

  @Test
  void repeatingReturnsTheFirstDecision() throws UnexpectedNoAccessRightException {
    controller.excludeAsAmbiguous(recallPrompt, exclusion("both tenses fit"));
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(3, 8).please());

    Answer answer =
        controller.excludeAsAmbiguous(recallPrompt, exclusion("another reason")).getAnswer();

    assertThat(answer.getExcludedAt(), equalTo(excludedAt));
    assertThat(answer.getExclusionReason(), equalTo("both tenses fit"));
  }

  @Test
  void leavesAnotherLearnersAttemptOnTheSameQuestionUntouched()
      throws UnexpectedNoAccessRightException {
    RecallPrompt othersPrompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(memoryTrackerOwnedByAnotherUser())
            .withMcq(recallPrompt.getMcq())
            .answerChoiceIndex(1)
            .please();

    controller.excludeAsAmbiguous(recallPrompt, exclusion("both tenses fit"));

    assertThat(makeMe.refresh(othersPrompt.getAnswer()).getExcludedAt(), nullValue());
  }

  @Test
  void refusesAnotherLearnersAttempt() {
    MemoryTracker othersTracker = memoryTrackerOwnedByAnotherUser();
    RecallPrompt othersPrompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(othersTracker)
            .withMcqForNote(othersTracker.getNote())
            .answerChoiceIndex(1)
            .please();

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.excludeAsAmbiguous(othersPrompt, exclusion("both tenses fit")));
  }

  @Test
  void refusesAnUnansweredPrompt() {
    RecallPrompt unanswered = mcqPrompt(recallPrompt.requireMemoryTracker(), note);

    assertThrows(
        QuestionAnswerException.class,
        () -> controller.excludeAsAmbiguous(unanswered, exclusion("both tenses fit")));
  }

  private AmbiguousExclusionDTO exclusion(String reason) {
    AmbiguousExclusionDTO dto = new AmbiguousExclusionDTO();
    dto.setReason(reason);
    return dto;
  }
}
