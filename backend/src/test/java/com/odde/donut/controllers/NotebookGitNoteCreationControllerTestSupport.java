package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;

abstract class NotebookGitNoteCreationControllerTestSupport
    extends NotebookGitBundleControllerTestBase {

  static NoteCreationDTO titleOnly(String title) {
    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle(title);
    return dto;
  }

  NotebookGitBinding binding(Notebook notebook) {
    return notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
  }

  /** The notebook's accepted state as its own boundaries currently report it. */
  AcceptedBinding acceptedBinding(Notebook notebook) throws Exception {
    NotebookGitBinding binding = binding(notebook);
    return new AcceptedBinding(
        binding.getAcceptedGitObjectId(), binding.getUpdatedAt(), acceptedHistory(notebook));
  }

  void assertBindingUnchanged(Notebook notebook, AcceptedBinding before) throws Exception {
    NotebookGitBinding after = binding(notebook);
    assertThat(after.getAcceptedGitObjectId(), is(before.acceptedHead()));
    assertThat(after.getUpdatedAt(), is(before.updatedAt()));
    assertThat(acceptedHistory(notebook), equalTo(before.acceptedHistory()));
  }

  record AcceptedBinding(
      String acceptedHead, Timestamp updatedAt, AcceptedHistory acceptedHistory) {}
}
