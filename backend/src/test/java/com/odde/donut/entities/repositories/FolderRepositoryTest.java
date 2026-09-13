package com.odde.donut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.util.List;
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
  @Autowired EntityManager entityManager;

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

    assertThat(loaded.getParentFolder().getId(), equalTo(parent.getId()));
  }

  @Test
  void persistsNoteWithFolderReference() {
    Notebook notebook = makeMe.aNotebook().please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Inbox").please();
    Note note = makeMe.aNote().folder(folder).please();
    makeMe.entityPersister.flush();

    Note loaded = noteRepository.findById(note.getId()).orElseThrow();

    assertThat(loaded.getFolder().getId(), equalTo(folder.getId()));
  }

  @Test
  void persistsNoteWithoutFolderWhenUnset() {
    Note note = makeMe.aNote().please();
    makeMe.entityPersister.flush();

    Note loaded = noteRepository.findById(note.getId()).orElseThrow();

    assertThat(loaded.getFolder(), nullValue());
  }

  @Test
  void mapsQueryMembershipFromCaseInsensitiveRootTrashAndItsDescendants() {
    Notebook notebook = makeMe.aNotebook().please();
    Folder trash = makeMe.aFolder().notebook(notebook).name("_TrAsH").please();
    Folder descendant = makeMe.aFolder().parentFolder(trash).name("Deep").please();
    Folder secondTrash = makeMe.aFolder().notebook(notebook).name("_TRASH").please();
    Folder ordinary = makeMe.aFolder().notebook(notebook).name("Ordinary").please();
    Folder nestedTrash = makeMe.aFolder().parentFolder(ordinary).name("_trash").please();
    makeMe.aFolder().notebook(notebook).name("Trash").please();
    Note descendantNote = makeMe.aNote().folder(descendant).please();
    Note secondTrashNote = makeMe.aNote().folder(secondTrash).please();
    makeMe.aNote().folder(nestedTrash).please();
    makeMe.aNote().notebook(notebook).please();
    makeMe.entityPersister.flushAndClear();

    List<Integer> trashedFolderIds =
        entityManager
            .createQuery(
                "SELECT f.id FROM Folder f WHERE f.trashedInDatabase = true ORDER BY f.id",
                Integer.class)
            .getResultList();
    List<Integer> trashedNoteIds =
        entityManager
            .createQuery(
                "SELECT n.id FROM Note n WHERE n.trashedInDatabase = true ORDER BY n.id",
                Integer.class)
            .getResultList();

    assertThat(
        trashedFolderIds, equalTo(List.of(trash.getId(), descendant.getId(), secondTrash.getId())));
    assertThat(trashedNoteIds, equalTo(List.of(descendantNote.getId(), secondTrashNote.getId())));
  }

  @Test
  void objectMembershipUsesCurrentAncestryWithinTheTransaction() {
    Notebook notebook = makeMe.aNotebook().please();
    Folder trash = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder movable = makeMe.aFolder().notebook(notebook).name("Movable").please();
    Note note = makeMe.aNote().folder(movable).please();
    makeMe.entityPersister.flush();

    movable.setParentFolder(trash);
    assertThat(movable.isTrashed(), equalTo(true));
    assertThat(note.isTrashed(), equalTo(true));

    movable.setParentFolder(null);
    assertThat(movable.isTrashed(), equalTo(false));
    assertThat(note.isTrashed(), equalTo(false));
  }
}
