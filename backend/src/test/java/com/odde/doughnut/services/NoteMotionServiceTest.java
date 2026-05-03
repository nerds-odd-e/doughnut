package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteMotionServiceTest {
  @Autowired NoteMotionService noteMotionService;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired NoteRepository noteRepository;

  @Autowired MakeMe makeMe;

  @Test
  void moveIntoFolder_detachesParentAndSetsFolder() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aRootNote("root").creatorAndOwner(user).please();
    Folder folder = makeMe.aFolder().notebook(root.getNotebook()).name("Dest").please();
    Note mover = makeMe.aNote("mover").creatorAndOwner(user).under(root).please();
    makeMe.entityPersister.flush();

    noteMotionService.executeMoveIntoFolder(mover, folder);

    makeMe.refresh(mover);
    assertThat(mover.getParent(), nullValue());
    assertThat(mover.getFolder().getId(), equalTo(folder.getId()));
    assertThat(mover.getNotebook().getId(), equalTo(root.getNotebook().getId()));
  }

  @Test
  void moveToTopLevel_clearsFolderAndDetachesParent() {
    User user = makeMe.aUser().please();
    Note topNote = makeMe.aRootNote("topNote").creatorAndOwner(user).please();
    Note firstChild = makeMe.aNote("middle").under(topNote).please();
    makeMe.entityPersister.flush();

    noteMotionService.moveToTopLevel(firstChild, user);

    makeMe.refresh(firstChild);
    assertThat(firstChild.getParent(), nullValue());
    assertThat(firstChild.getFolder(), nullValue());
  }

  @Test
  void moveToTopLevel_preservesNotebook() {
    User user = makeMe.aUser().please();
    Note topNote = makeMe.aRootNote("topNote").creatorAndOwner(user).please();
    Note firstChild = makeMe.aNote("middle").under(topNote).please();
    Integer notebookIdBefore = topNote.getNotebook().getId();
    makeMe.entityPersister.flush();

    noteMotionService.moveToTopLevel(firstChild, user);
    makeMe.entityPersister.flush();
    makeMe.refresh(firstChild);
    assertThat(firstChild.getNotebook().getId(), equalTo(notebookIdBefore));

    Integer notebookId = firstChild.getNotebook().getId();
    Long total =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM note WHERE notebook_id = ? AND deleted_at IS NULL",
            Long.class,
            notebookId);
    assertThat(total, equalTo(2L));
  }

  @Nested
  class ReorderInPlacement {
    @Test
    void ordersAfterPeerInFolder() throws MovementNotPossibleException {
      User user = makeMe.aUser().please();
      Note root = makeMe.aRootNote("root").creatorAndOwner(user).please();
      Folder folder = makeMe.aFolder().notebook(root.getNotebook()).name("box").please();
      Note n1 = makeMe.aNote("n1").creatorAndOwner(user).under(root).please();
      Note n2 = makeMe.aNote("n2").creatorAndOwner(user).under(root).please();
      Note mover = makeMe.aNote("mv").creatorAndOwner(user).under(root).please();
      makeMe.entityPersister.flush();
      noteMotionService.executeMoveIntoFolder(n1, folder);
      noteMotionService.executeMoveIntoFolder(n2, folder);
      noteMotionService.executeMoveIntoFolder(mover, folder);
      makeMe.entityPersister.flush();

      noteMotionService.executeReorderInPlacement(mover, folder, n1);
      List<Note> ordered = noteRepository.findNotesInFolderOrderBySiblingOrder(folder.getId());
      assertThat(
          ordered.stream().map(Note::getId).toList(),
          contains(n1.getId(), mover.getId(), n2.getId()));
    }

    @Test
    void firstAmongNotebookRootPeers() throws MovementNotPossibleException {
      User user = makeMe.aUser().please();
      Note topNote = makeMe.aRootNote("top").creatorAndOwner(user).please();
      Note firstChild = makeMe.aNote("r1").creatorAndOwner(user).under(topNote).please();
      Note secondChild = makeMe.aNote("r2").creatorAndOwner(user).under(topNote).please();
      makeMe.entityPersister.flush();
      noteMotionService.moveToTopLevel(firstChild, user);
      noteMotionService.moveToTopLevel(secondChild, user);
      makeMe.entityPersister.flush();
      makeMe.refresh(firstChild);
      makeMe.refresh(secondChild);

      noteMotionService.executeReorderInPlacement(secondChild, null, null);
      List<Note> ordered =
          noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(
              topNote.getNotebook().getId());
      assertThat(ordered.getFirst().getId(), equalTo(secondChild.getId()));
    }

    @Test
    void reorderAfterStructuralDescendant_throws() {
      User user = makeMe.aUser().please();
      Note root = makeMe.aRootNote("root").creatorAndOwner(user).please();
      Note parentMover = makeMe.aNote("parent").creatorAndOwner(user).under(root).please();
      Note child = makeMe.aNote("child").creatorAndOwner(user).under(parentMover).please();
      makeMe.entityPersister.flush();

      assertThrows(
          MovementNotPossibleException.class,
          () -> noteMotionService.executeReorderInPlacement(parentMover, null, child));
    }
  }
}
