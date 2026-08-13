package com.odde.doughnut.testability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.testability.model.NotesTestData;
import com.odde.doughnut.testability.model.NotesTestData.NoteTestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TestabilityInjectNotesSkipRecallTest {

  @Autowired TestabilityRestController testabilityRestController;
  @Autowired NoteRepository noteRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired MakeMe makeMe;

  @Test
  void injectNotes_skipMemoryTrackingSkipRecallsTheNote() {
    User user = makeMe.aUser().please();
    var data = new NotesTestData();
    data.setNotebookName("Skip inject nb");
    data.setExternalIdentifier(user.getExternalIdentifier());
    NoteTestData row = new NoteTestData();
    row.title = "English";
    row.setSkipMemoryTracking(true);
    data.setNoteTestData(List.of(row));

    Map<String, Integer> ids = testabilityRestController.injectNotes(data);
    Note note = noteRepository.findById(ids.get("English")).orElseThrow();
    List<MemoryTracker> trackers =
        memoryTrackerRepository.findByUserAndNote(user.getId(), note.getId());

    assertThat(trackers, hasSize(1));
    assertThat(trackers.get(0).getRemovedFromTracking(), equalTo(true));
    assertThat(note.getRecallSetting().getSkipMemoryTracking(), equalTo(false));
  }
}
