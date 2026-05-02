package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
  void moveToTopLevelClearsRootFolderAndAlignsDescendantFoldersWhenChildContainerExists() {
    User user = makeMe.aUser().please();
    Note topNote = makeMe.aRootNote("topNote").creatorAndOwner(user).please();
    Note firstChild = makeMe.aNote("middle").under(topNote).please();
    Note grandChild = makeMe.aNote("leaf").under(firstChild).please();
    makeMe.entityPersister.flush();
    Folder childContainer =
        makeMe.aFolder().notebook(topNote.getNotebook()).name("middle").parentFolder(null).please();
    makeMe.entityPersister.flush();

    noteMotionService.moveToTopLevel(firstChild, user);

    makeMe.refresh(firstChild);
    makeMe.refresh(grandChild);
    makeMe.refresh(topNote);
    assertThat(firstChild.getParent(), nullValue());
    assertThat(firstChild.getFolder(), nullValue());
    assertThat(grandChild.getFolder(), notNullValue());
    assertThat(grandChild.getFolder().getId(), equalTo(childContainer.getId()));
  }

  @Test
  void moveToTopLevelClearsDescendantFolderWhenNoMatchingChildContainerFolder() {
    User user = makeMe.aUser().please();
    Note topNote = makeMe.aRootNote("topNote").creatorAndOwner(user).please();
    Note firstChild = makeMe.aNote("middle").under(topNote).please();
    Note grandChild = makeMe.aNote("leaf").under(firstChild).please();
    makeMe.entityPersister.flush();

    noteMotionService.moveToTopLevel(firstChild, user);

    makeMe.refresh(firstChild);
    makeMe.refresh(grandChild);
    assertThat(firstChild.getFolder(), nullValue());
    assertThat(grandChild.getFolder(), nullValue());
  }

  @Test
  void moveToTopLevel_keepsAllNotesInNotebook() {
    User user = makeMe.aUser().please();
    Note topNote = makeMe.aRootNote("topNote").creatorAndOwner(user).please();
    Note firstChild = makeMe.aNote("middle").under(topNote).please();
    Note grandChild = makeMe.aNote("leaf").under(firstChild).please();
    Integer notebookIdBefore = topNote.getNotebook().getId();
    makeMe.entityPersister.flush();

    noteMotionService.moveToTopLevel(firstChild, user);
    makeMe.entityPersister.flush();
    makeMe.refresh(firstChild);
    makeMe.refresh(grandChild);
    makeMe.refresh(topNote);
    assertThat(firstChild.getNotebook().getId(), equalTo(notebookIdBefore));

    Integer notebookId = firstChild.getNotebook().getId();
    Long total =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM note WHERE notebook_id = ? AND deleted_at IS NULL",
            Long.class,
            notebookId);
    assertThat(total, equalTo(3L));
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
  }
}
