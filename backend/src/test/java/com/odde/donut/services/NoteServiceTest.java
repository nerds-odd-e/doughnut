package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteServiceTest {
  @Autowired MakeMe makeMe;
  @Autowired NoteService noteService;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void restore_makes_all_trackers_on_the_note_available_preserving_identities_and_preferences() {
    User owner = makeMe.aUser().please();
    Note note = makeMe.aNote().notebookOwnedBy(owner).please();
    MemoryTracker understandingTracker = makeMe.aMemoryTrackerFor(note).please();
    MemoryTracker spellingTracker = makeMe.aMemoryTrackerFor(note).spelling().please();
    MemoryTracker removedTracker =
        makeMe.aMemoryTrackerFor(note).propertyKey("summary").removedFromTracking().please();

    noteService.destroy(note, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, owner);
    noteService.restore(note, owner);

    List<MemoryTracker> trackers = memoryTrackerRepository.findByNote_IdIn(List.of(note.getId()));
    assertThat(trackers, hasSize(3));
    assertThat(
        trackers.stream()
            .filter(mt -> mt.getId().equals(understandingTracker.getId()))
            .findFirst()
            .orElseThrow()
            .isActive(),
        is(true));
    assertThat(
        trackers.stream()
            .filter(mt -> mt.getId().equals(spellingTracker.getId()))
            .findFirst()
            .orElseThrow()
            .isActive(),
        is(true));
    assertThat(
        trackers.stream()
            .filter(mt -> mt.getId().equals(removedTracker.getId()))
            .findFirst()
            .orElseThrow()
            .isActive(),
        is(false));
  }
}
