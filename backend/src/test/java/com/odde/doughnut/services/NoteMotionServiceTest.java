package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteMotionServiceTest {
  @Autowired NoteMotionService noteMotionService;
  @Autowired NoteRepository noteRepository;

  @Autowired MakeMe makeMe;

  @Test
  void executeMoveIntoFolder_setsFolderAndNotebook() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aRootNote("root").notebookCreatorAndOwner(user).please();
    Folder folder = makeMe.aFolder().notebook(root.getNotebook()).name("Dest").please();
    Note mover =
        makeMe.aNote("mover").notebookCreatorAndOwner(user).underSameNotebookAs(root).please();
    makeMe.entityPersister.flush();

    noteMotionService.executeMoveIntoFolder(mover, folder);

    makeMe.refresh(mover);
    assertThat(mover.getFolder().getId(), equalTo(folder.getId()));
    assertThat(mover.getNotebook().getId(), equalTo(root.getNotebook().getId()));
  }

  @Test
  void executeMoveIntoFolder_includesNoteAmongFolderPeers() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aRootNote("root").notebookCreatorAndOwner(user).please();
    Folder folder = makeMe.aFolder().notebook(root.getNotebook()).name("box").please();
    Note n1 = makeMe.aNote("n1").notebookCreatorAndOwner(user).underSameNotebookAs(root).please();
    Note n2 = makeMe.aNote("n2").notebookCreatorAndOwner(user).underSameNotebookAs(root).please();
    Note mover =
        makeMe.aNote("mv").notebookCreatorAndOwner(user).underSameNotebookAs(root).please();
    makeMe.entityPersister.flush();
    noteMotionService.executeMoveIntoFolder(n1, folder);
    noteMotionService.executeMoveIntoFolder(n2, folder);
    makeMe.entityPersister.flush();

    noteMotionService.executeMoveIntoFolder(mover, folder);
    List<Note> ordered = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
    assertThat(
        ordered.stream().map(Note::getId).toList(),
        containsInAnyOrder(n1.getId(), n2.getId(), mover.getId()));
  }

  @Test
  void executeMoveToNotebookRoot_placesNoteInNotebookRoot() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aRootNote("root").notebookCreatorAndOwner(user).please();
    makeMe.aNote("peer").notebookCreatorAndOwner(user).underSameNotebookAs(root).please();
    Folder folder = makeMe.aFolder().notebook(root.getNotebook()).name("f").please();
    Note mover =
        makeMe.aNote("mv").notebookCreatorAndOwner(user).underSameNotebookAs(root).please();
    makeMe.entityPersister.flush();
    noteMotionService.executeMoveIntoFolder(mover, folder);
    makeMe.entityPersister.flush();

    noteMotionService.executeMoveToNotebookRoot(mover);
    makeMe.refresh(mover);
    assertThat(mover.getFolder(), nullValue());
    List<Note> rootScope =
        noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(root.getNotebook().getId());
    assertThat(rootScope.stream().map(Note::getId).toList(), hasItem(equalTo(mover.getId())));
  }
}
