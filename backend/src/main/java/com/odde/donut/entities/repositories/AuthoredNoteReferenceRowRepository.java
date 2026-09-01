package com.odde.donut.entities.repositories;

import com.odde.donut.entities.AuthoredNoteReferenceRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Internal to the note-reference persistence boundary: {@link AuthoredNoteReferenceRow} is a
 * persistence row, never exposed to consumers outside this package. Domain references and
 * resolutions are exposed instead (see {@code com.odde.donut.algorithms.AuthoredNoteReference}).
 * Kept package-private so it cannot be injected or queried from outside by mistake.
 */
interface AuthoredNoteReferenceRowRepository
    extends JpaRepository<AuthoredNoteReferenceRow, Integer> {

  List<AuthoredNoteReferenceRow> findByNote_IdOrderByDocumentOrderAsc(Integer noteId);
}
