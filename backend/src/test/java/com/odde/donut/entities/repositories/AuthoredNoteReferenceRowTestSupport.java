package com.odde.donut.entities.repositories;

import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import java.util.List;

final class AuthoredNoteReferenceRowTestSupport {
  private AuthoredNoteReferenceRowTestSupport() {}

  static List<AuthoredNoteReferenceRow> rowsFor(
      AuthoredNoteReferenceRowRepository authoredNoteReferenceRowRepository, Note note) {
    return authoredNoteReferenceRowRepository.findByNote_IdOrderByDocumentOrderAsc(note.getId());
  }
}
