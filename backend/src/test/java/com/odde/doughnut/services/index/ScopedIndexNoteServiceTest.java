package com.odde.doughnut.services.index;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Folder;
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
class ScopedIndexNoteServiceTest {

  @Autowired ScopedIndexNoteService scopedIndexNoteService;
  @Autowired MakeMe makeMe;

  @Test
  void findDesignatedIndexNote_forFolderScope_returnsEmpty() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folder = makeMe.aFolder().notebook(notebook).please();

    Optional<Note> result =
        scopedIndexNoteService.findDesignatedIndexNote(new IndexScope.FolderIndex(folder));

    assertThat(result.isEmpty(), is(true));
  }

  @Test
  void isDesignatedIndexNote_notebookRoot_requiresRootScopeWithoutFolder() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note rootIndex =
        makeMe.aNote().creatorAndOwner(owner).inNotebook(notebook).title("index").please();
    Folder folder = makeMe.aFolder().notebook(notebook).please();
    Note inFolder = makeMe.aNote().creatorAndOwner(owner).folder(folder).title("index").please();

    assertThat(
        scopedIndexNoteService.isDesignatedIndexNote(
            new IndexScope.NotebookRoot(notebook), rootIndex),
        is(true));
    assertThat(rootIndex.getFolder(), is(nullValue()));
    assertThat(
        scopedIndexNoteService.isDesignatedIndexNote(
            new IndexScope.NotebookRoot(notebook), inFolder),
        is(false));
  }

  @Test
  void isDesignatedIndexNote_folderScope_requiresSameNotebookAndFolder() {
    User owner = makeMe.aUser().please();
    Notebook notebookA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook notebookB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folderA1 = makeMe.aFolder().notebook(notebookA).please();
    Folder folderA2 = makeMe.aFolder().notebook(notebookA).please();
    Note inFolderA1 =
        makeMe.aNote().creatorAndOwner(owner).folder(folderA1).title("index").please();
    Note inFolderA2 =
        makeMe.aNote().creatorAndOwner(owner).folder(folderA2).title("index").please();
    Note inNotebookBRoot =
        makeMe.aNote().creatorAndOwner(owner).inNotebook(notebookB).title("index").please();

    assertThat(
        scopedIndexNoteService.isDesignatedIndexNote(
            new IndexScope.FolderIndex(folderA1), inFolderA1),
        is(true));
    assertThat(
        scopedIndexNoteService.isDesignatedIndexNote(
            new IndexScope.FolderIndex(folderA1), inFolderA2),
        is(false));
    assertThat(
        scopedIndexNoteService.isDesignatedIndexNote(
            new IndexScope.FolderIndex(folderA1), inNotebookBRoot),
        is(false));
  }
}
