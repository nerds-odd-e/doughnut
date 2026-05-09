package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotebookRepository;
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
  @Autowired NotebookRepository notebookRepository;
  @Autowired MakeMe makeMe;

  @Test
  void findOptionalIndexNote_whenNoIndexNote_returnsEmpty() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();

    Optional<Note> result = notebookService.findOptionalIndexNote(notebook);

    assertThat(result.isEmpty(), is(true));
  }

  @Test
  void findOptionalIndexNote_whenRootNoteTitleIsIndex_findsNote() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note root = makeMe.aNote().inNotebook(notebook).creatorAndOwner(owner).please();
    makeMe.theNote(root).title("index").please();

    Optional<Note> result = notebookService.findOptionalIndexNote(notebook);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getId(), equalTo(root.getId()));
  }

  @Test
  void findOptionalIndexNote_whenTitleIsIndexCaseInsensitive_findsNote() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note root = makeMe.aNote().inNotebook(notebook).creatorAndOwner(owner).please();
    makeMe.theNote(root).title("Index").please();

    Optional<Note> result = notebookService.findOptionalIndexNote(notebook);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getId(), equalTo(root.getId()));
  }

  @Test
  void findOptionalIndexNote_whenCachedPointerValid_returnsEvenWhenTitleNotLiteralIndex() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note landing =
        makeMe.aNote().creatorAndOwner(owner).inNotebook(notebook).title("Welcome").please();
    makeMe.theNotebook(notebook).indexNote(landing).please();
    makeMe.entityPersister.flush();

    Notebook managed = notebookRepository.findById(notebook.getId()).orElseThrow();
    Optional<Note> result = notebookService.findOptionalIndexNote(managed);

    assertThat(result.map(Note::getId), equalTo(Optional.of(landing.getId())));
  }

  @Test
  void findOptionalIndexNote_whenCachedPointerInvalid_repairsFromRootIndexTitle() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folder = makeMe.aFolder().notebook(notebook).please();
    Note wrong = makeMe.aNote().creatorAndOwner(owner).folder(folder).title("misc").please();
    Note indexNote =
        makeMe.aNote().creatorAndOwner(owner).inNotebook(notebook).title("index").please();
    makeMe.theNotebook(notebook).indexNote(wrong).please();
    makeMe.entityPersister.flush();

    Notebook managed = notebookRepository.findById(notebook.getId()).orElseThrow();
    Optional<Note> result = notebookService.findOptionalIndexNote(managed);

    assertThat(result.map(Note::getId), equalTo(Optional.of(indexNote.getId())));
    Notebook updated = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(updated.getIndexNote().getId(), equalTo(indexNote.getId()));
  }
}
