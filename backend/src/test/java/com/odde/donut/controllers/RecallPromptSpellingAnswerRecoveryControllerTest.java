package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.AnsweredQuestion;
import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.server.ResponseStatusException;

class RecallPromptSpellingAnswerRecoveryControllerTest extends RecallPromptAnswerRecoveryTestBase {
  enum Outcome {
    CORRECT,
    WRONG,
    OVERLAP,
    UNIQUE_MATCH,
    MULTIPLE_MATCH
  }

  @ParameterizedTest
  @EnumSource(Outcome.class)
  void repeatedRecoveryPreservesAcceptedSpellingAndLearningEffectsAfterNoteEdits(Outcome outcome) {
    RecallPrompt prompt = createSpellingPrompt();
    List<Integer> matchedTrackers = new ArrayList<>();
    List<Integer> matchedNotes = new ArrayList<>();
    String spelling = outcome == Outcome.CORRECT ? "Correct" : "Other";
    inCommittedTransaction(
        transactionManager,
        () -> {
          currentUser.setUser(entityManager.find(User.class, currentUser.getUser().getId()));
          Note reviewed = repository.findById(prompt.getId()).orElseThrow().getNote();
          if (outcome == Outcome.OVERLAP
              || outcome == Outcome.UNIQUE_MATCH
              || outcome == Outcome.MULTIPLE_MATCH) {
            Note match =
                makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(spelling).please();
            matchedNotes.add(match.getId());
            matchedTrackers.add(ownedSpellingTracker(match).getId());
            if (outcome == Outcome.OVERLAP) makeMe.theNote(reviewed).overlapPartner(match).please();
            if (outcome == Outcome.MULTIPLE_MATCH) {
              Note second =
                  makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(spelling).please();
              matchedNotes.add(second.getId());
              matchedTrackers.add(ownedSpellingTracker(second).getId());
            }
          }
        });
    TrackerState before = trackerState(prompt.requireMemoryTracker().getId());
    List<TrackerState> matchedBefore = matchedTrackers.stream().map(this::trackerState).toList();
    inCommittedTransaction(
        transactionManager,
        () -> acceptSpelling(repository.findById(prompt.getId()).orElseThrow(), spelling, true));
    Evidence original = evidence(prompt.getId());
    List<TrackerState> matchedAfter = matchedTrackers.stream().map(this::trackerState).toList();
    assertThat(original.answer().get("spellingAnswer").textValue(), equalTo(spelling));
    assertThat(
        original.answer().get("correct").booleanValue(),
        is(outcome == Outcome.CORRECT || outcome == Outcome.OVERLAP));
    assertThat(original.answer().get("thinkingTimeMs").intValue(), equalTo(5000));
    assertThat(original.answer().get("awayMs").intValue(), equalTo(1000));
    assertThat(original.answer().get("awayCount").intValue(), equalTo(2));
    assertThat(original.answer().get("detourMs").intValue(), equalTo(2000));
    assertThat(original.answer().get("detourCount").intValue(), equalTo(3));
    assertThat(original.answer().get("idleMs").intValue(), equalTo(500));
    if (outcome == Outcome.OVERLAP) {
      assertThat(
          original.answer().get("outcome").textValue(), equalTo(AnswerOutcome.OVERLAP.name()));
      assertThat(trackerState(prompt.requireMemoryTracker().getId()), equalTo(before));
      assertThat(matchedAfter, equalTo(matchedBefore));
    } else {
      assertThat(original.tracker().logs().size(), equalTo(1));
      assertThat(
          original.tracker().logs().get(0).get("productOutcome").textValue(),
          equalTo(outcome == Outcome.CORRECT ? "GOOD" : "AGAIN"));
      assertThat(original.tracker().recallCount(), equalTo(before.recallCount() + 1));
      if (outcome == Outcome.UNIQUE_MATCH || outcome == Outcome.MULTIPLE_MATCH) {
        assertThat(
            original.answer().get("outcome").textValue(),
            equalTo(AnswerOutcome.ACCIDENTAL_MATCH.name()));
        if (outcome == Outcome.UNIQUE_MATCH) {
          assertThat(matchedAfter.getFirst().logs().size(), equalTo(1));
          assertThat(
              matchedAfter.getFirst().logs().get(0).get("productOutcome").textValue(),
              equalTo("CONFUSION"));
          assertThat(
              matchedAfter.getFirst().logs().get(0).get("answerId"),
              equalTo(original.answer().get("id")));
        } else assertThat(matchedAfter, equalTo(matchedBefore));
      }
    }
    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reviewed = repository.findById(prompt.getId()).orElseThrow().getNote();
          makeMe
              .theNote(reviewed)
              .title(outcome == Outcome.CORRECT ? "Edited" : spelling)
              .content("Changed after acceptance")
              .please();
        });
    testabilitySettings.timeTravelTo(new Timestamp(original.createdAt().getTime() + 86_400_000));
    for (String retry : new String[] {spelling, "Changed retry", spelling}) {
      inCommittedTransaction(
          transactionManager,
          () -> {
            AnsweredQuestion recovered =
                acceptSpelling(repository.findById(prompt.getId()).orElseThrow(), retry, false);
            assertThat(objectMapper.valueToTree(recovered.getAnswer()), equalTo(original.answer()));
            assertThat(recovered.getAnswer().getCreatedAt(), equalTo(original.createdAt()));
            if (outcome == Outcome.UNIQUE_MATCH || outcome == Outcome.MULTIPLE_MATCH) {
              assertThat(
                  recovered.getMatchedNotes().stream().map(match -> match.getId()).toList(),
                  equalTo(matchedNotes));
              assertThat(
                  recovered.getMatchedNotes().stream().map(match -> match.getTitle()).toList(),
                  everyItem(equalTo(spelling)));
            }
          });
    }
    assertThat(evidence(prompt.getId()), equalTo(original));
    assertThat(matchedTrackers.stream().map(this::trackerState).toList(), equalTo(matchedAfter));
  }

  @Test
  void acceptedMatchUsesCurrentReadAuthorizationAndOriginalOutcome() {
    RecallPrompt prompt = createSpellingPrompt();
    Note match =
        inCommittedTransaction(
            transactionManager,
            () -> makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Other").please());
    inCommittedTransaction(
        transactionManager,
        () -> acceptSpelling(repository.findById(prompt.getId()).orElseThrow(), "Other", true));
    Evidence original = evidence(prompt.getId());
    User owner = currentUser.getUser();
    User other =
        inCommittedTransaction(
            transactionManager, () -> makeMe.aUser(fixtureUser + "-other").please());
    try {
      inCommittedTransaction(
          transactionManager,
          () -> {
            makeMe
                .theNote(entityManager.find(Note.class, match.getId()))
                .notebookOwnership(other)
                .please();
            AnsweredQuestion recovered =
                acceptSpelling(repository.findById(prompt.getId()).orElseThrow(), "Correct", false);
            assertThat(recovered.getMatchedNotes(), nullValue());
            assertThat(objectMapper.valueToTree(recovered.getAnswer()), equalTo(original.answer()));
          });
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class,
          () -> controller.answerSpelling(prompt, spellingAnswer("Correct")));
      currentUser.setUser(other);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.answerSpelling(prompt, spellingAnswer("Correct")));
      assertThat(evidence(prompt.getId()), equalTo(original));
    } finally {
      currentUser.setUser(owner);
      inCommittedTransaction(
          transactionManager,
          () -> {
            makeMe
                .theNote(entityManager.find(Note.class, match.getId()))
                .notebookOwnership(owner)
                .please();
            deleteByUserExternalIdentifierLike(entityManager, other.getExternalIdentifier());
          });
    }
  }
}
