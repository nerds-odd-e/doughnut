package com.odde.donut.entities.repositories;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.controllers.ControllerTestBase;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the content-save HTTP boundary ({@code PATCH /api/text_content/{note}/content}) and
 * asserts what lands in {@code authored_note_reference}: one source-owned row per distinct authored
 * reference, unaffected by whether a wiki reference resolves, is missing, or is ambiguous (ADR 0001
 * Wiki link) — parsing is pure and does not resolve destinations.
 *
 * <p>Lives in {@code entities.repositories} (not {@code controllers}, alongside its {@code
 * TextContentController…Tests} siblings) so it can {@code @Autowired} the package-private {@link
 * AuthoredNoteReferenceRowRepository}, while still driving the save through {@link MockMvc} to
 * exercise the real HTTP boundary.
 */
@AutoConfigureMockMvc
class TextContentControllerAuthoredReferencePersistenceTest extends ControllerTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthoredNoteReferenceRowRepository authoredNoteReferenceRowRepository;

  private void saveContent(Note note, String content) throws Exception {
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent(content);
    mockMvc
        .perform(
            patch("/api/text_content/{note}/content", note.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk());
  }

  @Test
  void
      savingMixedAuthoredReferencesPersistsOneSourceOwnedRowPerDistinctReferenceAndReplacesOnChange()
          throws Exception {
    currentUser.setUser(makeMe.aUser().please());
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

    saveContent(carrier, content);

    List<AuthoredNoteReferenceRow> rows = rowsFor(authoredNoteReferenceRowRepository, carrier);
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

    saveContent(carrier, "[[Resolved]]");

    List<AuthoredNoteReferenceRow> rowsAfterChange =
        rowsFor(authoredNoteReferenceRowRepository, carrier);
    assertThat(rowsAfterChange, hasSize(1));
    assertThat(rowsAfterChange.getFirst().getAuthoredLink(), equalTo("Resolved"));
    assertThat(rowsFor(authoredNoteReferenceRowRepository, resolved), hasSize(0));
  }
}
