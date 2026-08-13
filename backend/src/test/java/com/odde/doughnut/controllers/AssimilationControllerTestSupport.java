package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;

final class AssimilationControllerTestSupport {
  private AssimilationControllerTestSupport() {}

  static Note ownedNote(MakeMe makeMe, User user, String title) {
    return makeMe.aNote(title).notebookOwnedBy(user).please();
  }

  static AssimilationRequestDTO assimilateRequest(Note note) {
    AssimilationRequestDTO request = new AssimilationRequestDTO();
    request.noteId = note.getId();
    return request;
  }

  static AssimilationRequestDTO assimilatePropertyRequest(Note note, String propertyKey) {
    AssimilationRequestDTO request = assimilateRequest(note);
    request.propertyKey = propertyKey;
    return request;
  }

  static AssimilationRequestDTO assimilateCommissionedRequest(Note note) {
    AssimilationRequestDTO request = assimilateRequest(note);
    request.assimilateAsCommissioned = true;
    return request;
  }

  static AssimilationRequestDTO assimilateSpellingRequest(Note note) {
    AssimilationRequestDTO request = assimilateRequest(note);
    request.assimilateAsSpelling = true;
    return request;
  }
}
