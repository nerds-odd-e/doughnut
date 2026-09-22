package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Note;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.NotebookProjectionChange;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.ProjectionChange;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.ProjectionRow;
import com.odde.donut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectionChangeCaptureTest {

  @Autowired MakeMe makeMe;
  @Autowired EntityPersister entityPersister;
  @Autowired ProjectionChangeCapture capture;

  @Test
  void recordsOneNoteUpdateWithTheFirstSeenPreviousTitleOnlyWhileTheWindowIsOpen() {
    Note note = makeMe.aNote().title("Before").please();
    note.setTitle(new DisplayName("Outside"));
    entityPersister.flush();

    NotebookProjectionChange recorded;
    try (ProjectionChange change = capture.open()) {
      note.setTitle(new DisplayName("Inside"));
      entityPersister.flush();
      note.setTitle(new DisplayName("Again"));
      entityPersister.flush();
      recorded = change.of(note.getNotebook().getId());
    }
    makeMe.aNote().underSameNotebookAs(note).please();
    entityPersister.flush();

    ProjectionRow row = new ProjectionRow(Note.class, note.getId());
    assertThat(recorded.updated.keySet(), contains(row));
    assertThat(recorded.updated.get(row).name(), equalTo("Outside"));
    assertThat(recorded.inserted, empty());
  }
}
