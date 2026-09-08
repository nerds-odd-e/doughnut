package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;

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

  void assertBindingUnchanged(Notebook notebook, NotebookGitBinding before) {
    NotebookGitBinding after = binding(notebook);
    assertThat(after.getAcceptedGitObjectId(), is(before.getAcceptedGitObjectId()));
    assertThat(after.getBundleBytes(), equalTo(before.getBundleBytes()));
    assertThat(after.getUpdatedAt(), is(before.getUpdatedAt()));
  }
}
