package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Folder;
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
}
