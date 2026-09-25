package com.odde.donut.entities;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.odde.donut.testability.SpringTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NotebookTest extends SpringTestBase {
  Notebook notebook;

  @BeforeEach
  void setup() {
    User user = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    makeMe.aNote().notebook(notebook).please();
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
    void shouldIncludeTrashedNotesInNotebook() {
      makeMe.aNote().notebook(notebook).trashed().please();
      makeMe.refresh(notebook);
      assertThat(notebook.getNotes().size()).isEqualTo(2);
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
