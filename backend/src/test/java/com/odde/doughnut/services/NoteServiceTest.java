package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteServiceTest {
  @Autowired MakeMe makeMe;
  @Autowired NoteService noteService;
  @Autowired UserService userService;

  @Nested
  class Destroy {
    @Test
    void shouldSoftDeleteMemoryTrackersWhenNoteIsDeleted() {
      Note note = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(note.getCreator()).please();

      noteService.destroy(note);

      MemoryTracker refreshed =
          makeMe.entityPersister.find(MemoryTracker.class, memoryTracker.getId());
      assertThat(refreshed.getDeletedAt(), notNullValue());
    }

    @Test
    void shouldExcludeSoftDeletedMemoryTrackersFromGetMemoryTrackersFor() {
      Note note = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
      makeMe.aMemoryTrackerFor(note).by(note.getCreator()).please();

      noteService.destroy(note);

      assertThat(userService.getMemoryTrackersFor(note.getCreator(), note), hasSize(0));
    }
  }

  @Nested
  class Restore {
    @Test
    void shouldRestoreOnlyMemoryTrackersWithSameDeletedAt() {
      Timestamp t1 = makeMe.aTimestamp().of(1, 0).please();
      Timestamp t2 = TimestampOperations.addHoursToTimestamp(t1, 1);

      Note note = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
      MemoryTracker mtDeletedAtT1 = makeMe.aMemoryTrackerFor(note).by(note.getCreator()).please();
      MemoryTracker mtDeletedAtT2 =
          makeMe.aMemoryTrackerFor(note).by(note.getCreator()).spelling().please();

      mtDeletedAtT1.setDeletedAt(t1);
      makeMe.entityPersister.merge(mtDeletedAtT1);
      note.setDeletedAt(t2);
      mtDeletedAtT2.setDeletedAt(t2);
      makeMe.entityPersister.merge(note);
      makeMe.entityPersister.merge(mtDeletedAtT2);

      noteService.restore(note);

      MemoryTracker refreshedT1 =
          makeMe.entityPersister.find(MemoryTracker.class, mtDeletedAtT1.getId());
      MemoryTracker refreshedT2 =
          makeMe.entityPersister.find(MemoryTracker.class, mtDeletedAtT2.getId());
      assertThat(refreshedT1.getDeletedAt(), notNullValue());
      assertThat(refreshedT2.getDeletedAt(), nullValue());
      assertThat(userService.getMemoryTrackersFor(note.getCreator(), note), hasSize(1));
    }
  }
}
