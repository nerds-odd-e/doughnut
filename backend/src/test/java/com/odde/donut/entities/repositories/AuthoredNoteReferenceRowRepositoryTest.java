package com.odde.donut.entities.repositories;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import com.odde.donut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthoredNoteReferenceRowRepositoryTest {

  @Autowired MakeMe makeMe;
  @Autowired AuthoredNoteReferenceRowRepository authoredNoteReferenceRowRepository;

  @Test
  void replaceContentPersistsOneRowPerReferenceInDocumentOrderAndReconstructsBothVariants() {
    Note note = makeMe.aNote().please();
    AuthoredNoteReference.WikiPortablePathTarget wiki =
        AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Notebook:Target Note");
    AuthoredNoteReference.NoteIdUrlTarget noteIdUrl =
        new AuthoredNoteReference.NoteIdUrlTarget("[Some Note](/n42)", 42, "/n42", "Some Note");

    note.replaceContent(new AuthoredNoteDocument("updated body", List.of(wiki, noteIdUrl)));
    makeMe.entityPersister.flush();

    List<AuthoredNoteReferenceRow> rows = rowsFor(authoredNoteReferenceRowRepository, note);
    assertThat(rows, hasSize(2));
    assertThat(rows.get(0).getDocumentOrder(), equalTo(0));
    assertThat(rows.get(1).getDocumentOrder(), equalTo(1));
    assertThat(rows.get(0).toDomainReference(), equalTo(wiki));
    assertThat(rows.get(1).toDomainReference(), equalTo(noteIdUrl));
    assertThat(note.getContent(), equalTo("updated body"));
  }

  @Test
  void replaceContentClearsPreviouslyAuthoredRows() {
    Note note = makeMe.aNote().please();
    note.replaceContent(
        new AuthoredNoteDocument(
            "first",
            List.of(new AuthoredNoteReference.NoteIdUrlTarget("[A](/n1)", 1, "/n1", "A"))));
    makeMe.entityPersister.flush();

    note.replaceContent(
        new AuthoredNoteDocument(
            "second",
            List.of(new AuthoredNoteReference.NoteIdUrlTarget("[B](/n2)", 2, "/n2", "B"))));
    makeMe.entityPersister.flush();

    List<AuthoredNoteReferenceRow> rows = rowsFor(authoredNoteReferenceRowRepository, note);
    assertThat(rows, hasSize(1));
    assertThat(rows.getFirst().getAuthoredLink(), equalTo("[B](/n2)"));
  }
}
