package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.testability.MakeMe;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
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
  @Autowired UserService userService;

  @Test
  void restore_only_restores_memory_trackers_with_same_deleted_at_as_note() {
    Timestamp t1 = makeMe.aTimestamp().of(1, 0).please();
    Timestamp t2 = TimestampOperations.addHoursToTimestamp(t1, 1);

    User owner = makeMe.aUser().please();
    Note note = makeMe.aNote().notebookOwnedBy(owner).please();
    MemoryTracker mtDeletedAtT1 = makeMe.aMemoryTrackerFor(note).please();
    MemoryTracker mtDeletedAtT2 = makeMe.aMemoryTrackerFor(note).spelling().please();

    mtDeletedAtT1.setDeletedAt(t1);
    makeMe.entityPersister.merge(mtDeletedAtT1);
    note.setDeletedAt(t2);
    mtDeletedAtT2.setDeletedAt(t2);
    makeMe.entityPersister.merge(note);
    makeMe.entityPersister.merge(mtDeletedAtT2);

    noteService.restore(note, owner);

    assertThat(
        makeMe.entityPersister.find(MemoryTracker.class, mtDeletedAtT1.getId()).getDeletedAt(),
        notNullValue());
    assertThat(
        makeMe.entityPersister.find(MemoryTracker.class, mtDeletedAtT2.getId()).getDeletedAt(),
        nullValue());
    assertThat(userService.getMemoryTrackersFor(owner, note), hasSize(1));
  }
}
