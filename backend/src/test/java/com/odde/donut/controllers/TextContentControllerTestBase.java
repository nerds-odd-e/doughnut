package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class TextContentControllerTestBase extends ControllerTestBase {
  @Autowired TextContentController controller;

  Note note;

  @BeforeEach
  void setupNote() {
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote("new").notebookOwnedBy(currentUser.getUser()).please();
  }

  protected NoteUpdateTitleDTO titleDto(String newTitle) {
    NoteUpdateTitleDTO dto = new NoteUpdateTitleDTO();
    dto.setNewTitle(newTitle);
    return dto;
  }

  protected NoteUpdateContentDTO contentDto(String content) {
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent(content);
    return dto;
  }

  protected record InboundWiki(Note target, Note carrier) {}

  protected InboundWiki noteWithInboundWiki(String targetTitle, String carrierContent)
      throws UnexpectedNoAccessRightException {
    Note target = makeMe.aNote().title(targetTitle).notebookOwnedBy(currentUser.getUser()).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(target).please();
    controller.updateNoteContent(carrier, contentDto(carrierContent));
    return new InboundWiki(target, carrier);
  }
}
