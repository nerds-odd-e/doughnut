package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
class FolderRepositoryTest {

  @Autowired MakeMe makeMe;
  @Autowired FolderRepository folderRepository;
  @Autowired NoteRepository noteRepository;

  @Test
  void persistsAndReloadsFolderInNotebook() {
    Notebook notebook = makeMe.aNotebook().please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Inbox").please();
    makeMe.entityPersister.flush();

    Folder loaded = folderRepository.findById(folder.getId()).orElseThrow();

    assertThat(loaded.getName(), equalTo("Inbox"));
    assertThat(loaded.getNotebook().getId(), equalTo(notebook.getId()));
    assertThat(loaded.getParentFolder(), nullValue());
    assertThat(loaded.getCreatedAt(), notNullValue());
    assertThat(loaded.getUpdatedAt(), notNullValue());
  }

  @Test
  void persistsNestedFolderWithParent() {
    Notebook notebook = makeMe.aNotebook().please();
    Folder parent = makeMe.aFolder().notebook(notebook).name("Parent").please();
    Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();
    makeMe.entityPersister.flush();

    Folder loaded = folderRepository.findById(child.getId()).orElseThrow();

    assertThat(loaded.getParentFolder(), notNullValue());
    assertThat(loaded.getParentFolder().getId(), equalTo(parent.getId()));
  }

  @Test
  void persistsNoteWithFolderReference() {
    Notebook notebook = makeMe.aNotebook().please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Inbox").please();
    Note note = makeMe.aNote().folder(folder).please();
    makeMe.entityPersister.flush();

    Note loaded = noteRepository.findById(note.getId()).orElseThrow();

    assertThat(loaded.getFolder(), notNullValue());
    assertThat(loaded.getFolder().getId(), equalTo(folder.getId()));
  }

  @Test
  void persistsNoteWithoutFolderWhenUnset() {
    Note note = makeMe.aNote().please();
    makeMe.entityPersister.flush();

    Note loaded = noteRepository.findById(note.getId()).orElseThrow();

    assertThat(loaded.getFolder(), nullValue());
    assertThat(loaded.getId(), notNullValue());
  }
}
