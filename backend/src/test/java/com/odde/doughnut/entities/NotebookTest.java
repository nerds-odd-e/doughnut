package com.odde.doughnut.entities;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotebookTest {
  @Autowired MakeMe makeMe;
  Note rootNote;
  Notebook notebook;

  @BeforeEach
  void setup() {
    notebook = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
    rootNote = makeMe.aNote().notebook(notebook).please();
  }

  @Nested
  class NotesManagementTests {
    @Test
    void shouldIncludeAllNonDeletedNotesInNotebook() {
      makeMe.aNote().notebook(notebook).please();
      makeMe.refresh(notebook);
      assertThat(notebook.getNotes().size()).isEqualTo(2);
    }

    @Test
    void shouldExcludeSoftDeletedNotesFromNotebook() {
      makeMe.aNote().notebook(notebook).softDeleted().please();
      makeMe.refresh(notebook);
      assertThat(notebook.getNotes().size()).isEqualTo(1);
    }
  }

  @Nested
  class NotebookMetadataTests {
    @Test
    void shouldReturnCorrectCreatorId() {
      assertThat(notebook.getCreatorId()).isEqualTo(notebook.getCreator().getExternalIdentifier());
    }
  }
}
