package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteFolderPersistenceTest {

  @Autowired MakeMe makeMe;
  @Autowired NoteRepository noteRepository;

  @Test
  void persistsAndReloadsNoteWithFolder() {
    Notebook notebook = makeMe.aNotebook().please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Topics").please();
    Note note = makeMe.aNote().creatorAndOwner(notebook.getCreatorEntity()).folder(folder).please();
    makeMe.entityPersister.flush();

    Note loaded = noteRepository.findById(note.getId()).orElseThrow();

    assertThat(loaded.getFolder(), equalTo(folder));
    assertThat(loaded.getFolder().getId(), equalTo(folder.getId()));
  }

  @Test
  void persistsNoteWithoutFolder() {
    Note note = makeMe.aNote().please();
    makeMe.entityPersister.flush();

    Note loaded = noteRepository.findById(note.getId()).orElseThrow();

    assertThat(loaded.getFolder(), nullValue());
  }
}
