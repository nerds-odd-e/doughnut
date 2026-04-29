package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotebookServiceTest {

  @Autowired NotebookService notebookService;
  @Autowired MakeMe makeMe;

  @Test
  void findOptionalIndexNote_whenNoIndexNote_returnsEmpty() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();

    Optional<Note> result = notebookService.findOptionalIndexNote(notebook);

    assertThat(result.isEmpty(), is(true));
  }

  @Test
  void findOptionalIndexNote_whenRootNoteSlugIsIndex_findsNote() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note head = notebook.getHeadNote();
    makeMe.theNote(head).title("Overview").slug("index").please();

    Optional<Note> result = notebookService.findOptionalIndexNote(notebook);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getId(), equalTo(head.getId()));
  }

  @Test
  void findOptionalIndexNote_whenTitleIsIndexCaseInsensitiveAndSlugIsNot_findsNote() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note head = notebook.getHeadNote();
    makeMe.theNote(head).title("Index").slug("notebook-landing").please();

    Optional<Note> result = notebookService.findOptionalIndexNote(notebook);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getId(), equalTo(head.getId()));
  }
}
