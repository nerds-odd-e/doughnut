package com.odde.doughnut.entities;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
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
  Note headNote;
  Notebook notebook;

  @BeforeEach
  void setup() {
    headNote = makeMe.aNote().please();
    notebook = headNote.getNotebook();
  }

  @Test
  void aNotebookWithHeadNoteAndAChild() {
    makeMe.aNote().under(headNote).please();
    makeMe.refresh(notebook);
    assertEquals(2, notebook.getNotes().size());
  }

  @Test
  void aNotebookWithHeadNoteAndADeletedChild() {
    makeMe.aNote().under(headNote).softDeleted().please();
    makeMe.refresh(notebook);
    assertEquals(1, notebook.getNotes().size());
  }

  @Test
  void creatorId() {
    assertThat(notebook.getCreatorId())
        .isEqualTo(notebook.getCreatorEntity().getExternalIdentifier());
  }

  @Test
  void certifiedBy() {
    assertThat(notebook.getCertifiedBy()).isEqualTo("Terry");
  }
}
