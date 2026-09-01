package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Content save persists one source-owned {@code authored_note_reference} row per distinct authored
 * reference, unaffected by whether a wiki reference resolves, is missing, or is ambiguous (ADR 0001
 * Wiki link) — parsing is pure and does not resolve destinations.
 */
class TextContentControllerAuthoredReferencePersistenceTest extends TextContentControllerTestBase {

  @Autowired EntityManager entityManager;

  @Test
  void
      savingMixedAuthoredReferencesPersistsOneSourceOwnedRowPerDistinctReferenceAndReplacesOnChange()
          throws UnexpectedNoAccessRightException {
    Note resolved =
        makeMe.aNote().title("Resolved").notebookOwnedBy(currentUser.getUser()).please();
    Folder folderA = makeMe.aFolder().notebook(resolved.getNotebook()).name("A").please();
    Folder folderB = makeMe.aFolder().notebook(resolved.getNotebook()).name("B").please();
    makeMe.aNote().title("Ambiguous").folder(folderA).please();
    makeMe.aNote().title("Ambiguous").folder(folderB).please();
    Note urlTarget = makeMe.aNote().title("Url Target").underSameNotebookAs(resolved).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(resolved).please();

    String content =
        "[[Resolved]] [[Missing]] [[Ambiguous]] [Some Label](/n" + urlTarget.getId() + ")";

    controller.updateNoteContent(carrier, contentDto(content));

    List<AuthoredNoteReferenceRow> rows = rowsFor(entityManager, carrier);
    assertThat(rows, hasSize(4));
    List<AuthoredNoteReference> references =
        rows.stream().map(AuthoredNoteReferenceRow::toDomainReference).toList();
    assertThat(
        references,
        equalTo(
            List.of(
                AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Resolved"),
                AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Missing"),
                AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Ambiguous"),
                new AuthoredNoteReference.NoteIdUrlTarget(
                    "[Some Label](/n" + urlTarget.getId() + ")",
                    urlTarget.getId(),
                    "/n" + urlTarget.getId(),
                    "Some Label"))));

    controller.updateNoteContent(carrier, contentDto("[[Resolved]]"));

    List<AuthoredNoteReferenceRow> rowsAfterChange = rowsFor(entityManager, carrier);
    assertThat(rowsAfterChange, hasSize(1));
    assertThat(rowsAfterChange.getFirst().getAuthoredLink(), equalTo("Resolved"));
    assertThat(rowsFor(entityManager, resolved), hasSize(0));
  }
}
