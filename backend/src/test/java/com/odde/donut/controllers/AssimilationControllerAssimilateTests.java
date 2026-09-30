package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.donut.controllers.dto.AssimilationRequestDTO;
import com.odde.donut.entities.*;
import com.odde.donut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.services.NotePropertyIndexService;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class AssimilationControllerAssimilateTests extends ControllerTestBase {
  @Autowired private MemoryTrackerRepository memoryTrackerRepository;
  @Autowired private AssimilationSequenceSkipRepository skipRepository;
  @Autowired AssimilationController controller;
  @Autowired NotePropertyIndexService notePropertyIndexService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class CreateAssimilationPoint {
    @Test
    void notLoggedIn() {
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.assimilate(new AssimilationRequestDTO()));
    }

    @Test
    void ordinaryAssimilateCreatesOnlyUnderstandingTracker() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      List<MemoryTracker> result =
          controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.UNDERSTANDING));
    }

    @Test
    void assimilatingSkippedNoteDeletesMatchingSkipRow() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.anAssimilationSequenceSkipFor(note).please();

      controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));

      assertThat(
          skipRepository.findByUserAndNoteAndPropertyKey(currentUser.getUser(), note, ""),
          is(Optional.empty()));
    }

    @Test
    void assimilatingSkippedPropertyDeletesMatchingSkipRow() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.anAssimilationSequenceSkipFor(note).propertyKey("a part of").please();

      controller.assimilate(
          AssimilationControllerTestSupport.assimilatePropertyValueRequest(
              note, "a part of", "[[Engine]]"));

      assertThat(
          skipRepository.findByUserAndNoteAndPropertyKey(currentUser.getUser(), note, "a part of"),
          is(Optional.empty()));
    }

    @Test
    void shouldReturnEmptyWhenNoteAlreadyHasMemoryTrackers() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).please();

      List<MemoryTracker> result =
          controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));

      assertThat(result, empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
    }

    @Test
    void assimilateLeavesLastRecallUnsetAndDueAtAssimilatedAt() {
      Timestamp now = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(now);
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      MemoryTracker tracker =
          controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note)).get(0);

      assertThat(tracker.getAssimilatedAt(), equalTo(now));
      assertThat(tracker.getLastRecalledAt(), nullValue());
      assertThat(tracker.getNextRecallAt(), equalTo(now));
    }

    @Test
    void shouldCreatePropertyTrackerWhenPropertyKeyProvided() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      List<MemoryTracker> result =
          controller.assimilate(
              AssimilationControllerTestSupport.assimilatePropertyRequest(note, "a part of"));

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getPropertyKey(), equalTo("a part of"));
      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.UNDERSTANDING));
    }

    @Test
    void assimilateDoesNotCreateTrackerRemovedFromRecall() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      assertThat(
          controller
              .assimilate(AssimilationControllerTestSupport.assimilateRequest(note))
              .get(0)
              .getRemovedFromTracking(),
          equalTo(false));
    }

    @Test
    void shouldReturnEmptyWhenPropertyTrackerAlreadyExists() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      AssimilationRequestDTO request =
          AssimilationControllerTestSupport.assimilatePropertyRequest(note, "a part of");
      controller.assimilate(request);

      assertThat(controller.assimilate(request), empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
    }

    @Test
    void shouldCoexistNoteLevelAndPropertyTrackersOnSameNote() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));
      controller.assimilate(
          AssimilationControllerTestSupport.assimilatePropertyRequest(note, "a part of"));

      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(2));
    }
  }

  @Nested
  class AssimilateOneListValue {
    Note note;

    @BeforeEach
    void setup() {
      note =
          makeMe
              .aNote()
              .notebookOwnedBy(currentUser.getUser())
              .content("---\nexample of:\n  - \"[[run]]\"\n  - \"[[past tense]]\"\n---\n\nbody")
              .please();
      notePropertyIndexService.refreshForNote(note);
    }

    private List<MemoryTracker> assimilateValue(String value) {
      return controller.assimilate(
          AssimilationControllerTestSupport.assimilatePropertyValueRequest(
              note, "example of", value));
    }

    @Test
    void createsOneTrackerWithTheKeyAndValue() {
      List<MemoryTracker> result = assimilateValue("[[run]]");

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getPropertyKey(), equalTo("example of"));
      assertThat(result.get(0).getPropertyValue(), equalTo("[[run]]"));
    }

    @Test
    void repeatingTheSameValueCreatesNothing() {
      assimilateValue("[[run]]");

      assertThat(assimilateValue("[[run]]"), empty());
    }

    @Test
    void theOtherValueStaysUnassimilated() {
      controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));
      assimilateValue("[[run]]");

      assertThat(
          controller.next("Asia/Shanghai").getNextUnit().getPropertyValue(),
          equalTo("[[past tense]]"));
    }

    @Test
    void theOtherValueCanStillBeAssimilated() {
      assimilateValue("[[run]]");

      assertThat(assimilateValue("[[past tense]]"), hasSize(1));
    }

    @Test
    void omittedValueCreatesAScalarTracker() {
      List<MemoryTracker> result =
          controller.assimilate(
              AssimilationControllerTestSupport.assimilatePropertyRequest(note, "example of"));

      assertThat(result.get(0).getPropertyValue(), equalTo(""));
    }
  }
}
