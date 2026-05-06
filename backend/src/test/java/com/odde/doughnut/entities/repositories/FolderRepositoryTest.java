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
    Folder child = makeMe.aFolder().notebook(notebook).parentFolder(parent).name("Child").please();
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

  void nestedFoldersMirrorContainmentHierarchyForNotes() {
    Notebook notebook = makeMe.aNotebook().please();
    Note root = makeMe.aNote().inNotebook(notebook).please();
    Folder folderForRootChildren =
        makeMe.aFolder().notebook(notebook).name(root.getTitle()).please();
    Folder folderForSectionChildren =
        makeMe
            .aFolder()
            .notebook(notebook)
            .parentFolder(folderForRootChildren)
            .name("Section")
            .please();

    Note section = makeMe.aNote().title("Section").folder(folderForRootChildren).please();
    Note leaf = makeMe.aNote().title("Leaf").folder(folderForSectionChildren).please();
    makeMe.entityPersister.flush();

    Note loadedSection = noteRepository.findById(section.getId()).orElseThrow();
    Note loadedLeaf = noteRepository.findById(leaf.getId()).orElseThrow();

    assertThat(loadedSection.getFolder().getId(), equalTo(folderForRootChildren.getId()));
    assertThat(loadedLeaf.getFolder().getId(), equalTo(folderForSectionChildren.getId()));
    assertThat(
        loadedLeaf.getFolder().getParentFolder().getId(), equalTo(folderForRootChildren.getId()));
  }
}
