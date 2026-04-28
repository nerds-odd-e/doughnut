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
  void folderMatchesContainerForParentWithChild() {
    Note parent = makeMe.aNote("Container").please();
    Note child = makeMe.aNote("Inside").under(parent).please();
    Folder folder =
        makeMe.aFolder().notebook(parent.getNotebook()).name(parent.getTitle()).please();
    makeMe.entityPersister.flush();

    Folder loaded = folderRepository.findById(folder.getId()).orElseThrow();
    Note reloadedParent = noteRepository.findById(parent.getId()).orElseThrow();
    Note reloadedChild = noteRepository.findById(child.getId()).orElseThrow();

    assertThat(loaded.getNotebook().getId(), equalTo(parent.getNotebook().getId()));
    assertThat(loaded.getName(), equalTo("Container"));
    assertThat(reloadedParent.getId(), equalTo(parent.getId()));
    assertThat(reloadedChild.getParent().getId(), equalTo(parent.getId()));
  }

  @Test
  void nestedFoldersMatchNestedParentsEachWithChildren() {
    Note grandparent = makeMe.aNote("Root").please();
    Note parent = makeMe.aNote("Branch").under(grandparent).please();
    makeMe.aNote("Leaf").under(parent).please();
    Folder folderRoot = makeMe.aFolder().notebook(grandparent.getNotebook()).name("Root").please();
    Folder folderBranch =
        makeMe
            .aFolder()
            .notebook(grandparent.getNotebook())
            .parentFolder(folderRoot)
            .name("Branch")
            .please();
    makeMe.entityPersister.flush();

    Folder loadedBranch = folderRepository.findById(folderBranch.getId()).orElseThrow();
    Note reloadedParent = noteRepository.findById(parent.getId()).orElseThrow();

    assertThat(loadedBranch.getParentFolder().getId(), equalTo(folderRoot.getId()));
    assertThat(reloadedParent.getParent().getId(), equalTo(grandparent.getId()));
  }

  @Test
  void childNotesKeepParentAndSiblingOrderWithFolderFromContainer() {
    Note parent = makeMe.aNote("Holder").please();
    Note first = makeMe.aNote("First").under(parent).please();
    Note second = makeMe.aNote("Second").after(first).please();
    Folder folder = makeMe.aFolder().notebook(parent.getNotebook()).name("Holder").please();
    first.setFolder(folder);
    second.setFolder(folder);
    noteRepository.save(first);
    noteRepository.save(second);
    makeMe.entityPersister.flush();

    Note reloadedFirst = noteRepository.findById(first.getId()).orElseThrow();
    Note reloadedSecond = noteRepository.findById(second.getId()).orElseThrow();

    assertThat(reloadedFirst.getFolder().getId(), equalTo(folder.getId()));
    assertThat(reloadedSecond.getFolder().getId(), equalTo(folder.getId()));
    assertThat(reloadedFirst.getParent().getId(), equalTo(parent.getId()));
    assertThat(reloadedSecond.getParent().getId(), equalTo(parent.getId()));
    assertThat(reloadedFirst.getSiblingOrder() < reloadedSecond.getSiblingOrder(), equalTo(true));
  }
}
