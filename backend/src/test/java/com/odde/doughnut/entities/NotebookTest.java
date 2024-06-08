package com.odde.doughnut.entities;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.testability.MakeMe;
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

  @Test
  void aNotebookWithHeadNoteAndAChild() {
    Note headNote = makeMe.aNote().please();
    Notebook notebook = headNote.getNotebook();
    makeMe.aNote().under(headNote).please();
    makeMe.refresh(notebook);
    assertEquals(2, notebook.getNotes().size());
  }

  @Test
  void aNotebookWithHeadNoteAndADeletedChild() {
    Note headNote = makeMe.aNote().please();
    Notebook notebook = headNote.getNotebook();
    makeMe.aNote().under(headNote).softDeleted().please();
    makeMe.refresh(notebook);
    assertEquals(1, notebook.getNotes().size());
  }
}
