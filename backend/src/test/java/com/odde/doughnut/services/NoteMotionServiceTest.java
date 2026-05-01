package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
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
  @Autowired NoteChildContainerFolderService noteChildContainerFolderService;
  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired MakeMe makeMe;
  Note topNote;
  Note firstChild;
  Note secondChild;

  @BeforeEach
  void setup() {
    topNote = makeMe.aRootNote("topNote").please();
    firstChild = makeMe.aNote("firstChild").under(topNote).please();
    secondChild = makeMe.aNote("secondChild").under(topNote).please();
  }

  void move(Note subject, Note relativeNote, boolean asFirstChildOfNote)
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    noteMotionService.validate(subject, relativeNote, asFirstChildOfNote);
    noteMotionService.execute(subject, relativeNote, asFirstChildOfNote);
  }

  private void alignFoldersForTestSubtree(Note root) {
    alignFolderForTest(root);
    root.getAllDescendants().forEach(this::alignFolderForTest);
  }

  private void alignFolderForTest(Note note) {
    Note parent = note.getParent();
    if (parent == null) {
      note.setFolder(null);
    } else {
      note.setFolder(noteChildContainerFolderService.resolveForParent(parent));
    }
  }

  @Test
  void moveBehind() throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(firstChild, secondChild, false);
    assertOrder(secondChild, firstChild);
  }

  @Test
  void moveBehindTop() {
    assertThrows(MovementNotPossibleException.class, () -> move(firstChild, topNote, false));
  }

  private void assertOrder(Note note1, Note note2) {
    Note parentNote = note1.getParent();
    makeMe.refresh(parentNote);
    assertThat(parentNote.getChildren(), containsInRelativeOrder(note1, note2));
  }

  @Test
  void moveSecondBehindFirstPreservesFolderWhenFolderUnchanged()
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    topNote = makeMe.aRootNote("top").please();
    Note section = makeMe.aNote("Section").under(topNote).please();
    firstChild = makeMe.aNote("firstChild").under(section).please();
    secondChild = makeMe.aNote("secondChild").under(section).please();
    makeMe.entityPersister.flush();
    alignFoldersForTestSubtree(section);
    makeMe.entityPersister.flush();
    makeMe.refresh(firstChild);
    makeMe.refresh(secondChild);
    assertThat(firstChild.getFolder().getId(), equalTo(secondChild.getFolder().getId()));

    int folderIdBefore = secondChild.getFolder().getId();
    move(secondChild, firstChild, false);
    makeMe.refresh(secondChild);
    assertThat(secondChild.getFolder().getId(), equalTo(folderIdBefore));
    assertOrder(firstChild, secondChild);
  }

  @Test
  void moveSecondToBeTheFirstSibling()
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(secondChild, topNote, true);
    assertOrder(secondChild, firstChild);
  }

  @Test
  void moveUnder() throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(firstChild, secondChild, true);
    assertThat(firstChild.getParent(), equalTo(secondChild));
    makeMe.refresh(firstChild);
    assertThat(firstChild.getFolder(), notNullValue());
    assertThat(firstChild.getFolder().getName(), equalTo(secondChild.getTitle()));
  }

  @Test
  void moveIntoParentWhereTitlesCollide_repParentsMoverUnderSection()
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    Note root = makeMe.aRootNote("top").please();
    Note section = makeMe.aNote("Section").under(root).please();
    makeMe.aNote("dup").under(section).please();
    Note second = makeMe.aNote("dup").under(section).please();
    Note otherRoot = makeMe.aRootNote("otherNb").please();
    Note mover = makeMe.aNote("dup").under(otherRoot).please();
    makeMe.entityPersister.flush();
    alignFoldersForTestSubtree(root);
    alignFoldersForTestSubtree(otherRoot);
    makeMe.entityPersister.flush();

    move(mover, second, false);

    makeMe.refresh(mover);
    assertThat(mover.getParent(), equalTo(section));
  }

  @Test
  void moveBothToTheEndInSequence()
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(firstChild, secondChild, false);
    move(secondChild, firstChild, false);
    assertOrder(firstChild, secondChild);
  }

  @Test
  void moveToTopLevelClearsRootFolderAndAlignsDescendantFolders() {
    User user = makeMe.aUser().please();
    topNote = makeMe.aRootNote("topNote").creatorAndOwner(user).please();
    firstChild = makeMe.aNote("middle").under(topNote).please();
    Note grandChild = makeMe.aNote("leaf").under(firstChild).please();
    Integer notebookIdBefore = topNote.getNotebook().getId();

    noteMotionService.moveToTopLevel(firstChild, user);

    makeMe.refresh(firstChild);
    makeMe.refresh(grandChild);
    makeMe.refresh(topNote);
    assertThat(firstChild.getNotebook().getId(), equalTo(notebookIdBefore));
    assertThat(firstChild.getParent(), nullValue());
    assertThat(firstChild.getFolder(), nullValue());
    assertThat(grandChild.getFolder(), notNullValue());
    assertThat(grandChild.getFolder().getName(), equalTo(firstChild.getTitle()));
  }

  @Test
  void moveToTopLevel_keepsAllNotesInNotebook() {
    User user = makeMe.aUser().please();
    topNote = makeMe.aRootNote("topNote").creatorAndOwner(user).please();
    firstChild = makeMe.aNote("middle").under(topNote).please();
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
  class WhenThereIsAThirdLevel {
    Note thirdLevel;
    Note forthLevel;

    @BeforeEach
    void setup() {
      thirdLevel = makeMe.aNote("thirdLevel").under(firstChild).please();
      forthLevel = makeMe.aNote("forthLevel").under(thirdLevel).please();
    }

    @Test
    void moveAfterNoteOfDifferentLevel()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      move(secondChild, thirdLevel, false);
      assertThat(secondChild.getParent(), equalTo(firstChild));
    }

    @Test
    void moveToOwnDescendentIsNotAllowed() {
      assertThrows(CyclicLinkDetectedException.class, () -> move(topNote, thirdLevel, false));
    }

    @Test
    void moveWithOwnChild() throws CyclicLinkDetectedException, MovementNotPossibleException {
      move(firstChild, secondChild, true);
      assertThat(firstChild.getAncestors(), contains(topNote, secondChild));
      assertThat(thirdLevel.getAncestors(), contains(topNote, secondChild, firstChild));
      assertThat(forthLevel.getAncestors(), contains(topNote, secondChild, firstChild, thirdLevel));
    }
  }

  @Nested
  class WhenThereIsAThirdChild {
    Note thirdChild;

    @BeforeEach
    void setup() {
      thirdChild = makeMe.aNote().under(topNote).please();
    }

    @Test
    void moveBetweenSecondAndThird()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      move(firstChild, secondChild, false);
      assertOrder(secondChild, firstChild);
      assertOrder(firstChild, thirdChild);
    }

    @Nested
    class WhenThereIsARelationshipNote {
      Note relationNote;

      @BeforeEach
      void setup() {
        relationNote = makeMe.aRelation().between(topNote, secondChild).please();
        makeMe.theNote(relationNote).after(firstChild).please();
        makeMe.refresh(topNote);
      }

      @Test
      void moveSecondToAfterFirstAndBeforeLinkNote()
          throws CyclicLinkDetectedException, MovementNotPossibleException {
        assertThat(relationNote.getSiblingOrder(), lessThan(secondChild.getSiblingOrder()));
        move(secondChild, firstChild, false);
        assertOrder(firstChild, secondChild);
        assertThat(relationNote.getSiblingOrder(), greaterThan(secondChild.getSiblingOrder()));
      }
    }
  }

  @Nested
  class WhenMovingBetweenNotebooks {
    Note otherNotebook;
    Note firstChild;
    Note secondChild;
    Note thirdLevel;

    @BeforeEach
    void setup() {
      topNote = makeMe.aRootNote("topForNotebookMove").please();
      otherNotebook = makeMe.aRootNote("otherNotebook").please();
      firstChild = makeMe.aNote("firstChild").under(topNote).please();
      secondChild = makeMe.aNote("secondChild").under(firstChild).please();
      thirdLevel = makeMe.aNote("thirdLevel").under(secondChild).please();
    }

    @Test
    void movingRootNoteIntoAnotherNotebookKeepsSourceNotebook()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      Integer destNotebookId = otherNotebook.getNotebook().getId();

      move(topNote, otherNotebook, true);

      makeMe.entityPersister.flush();
      makeMe.refresh(topNote);
      makeMe.refresh(firstChild);

      assertThat(topNote.getNotebook().getId(), equalTo(destNotebookId));
      assertThat(firstChild.getNotebook().getId(), equalTo(destNotebookId));
    }

    @Test
    void shouldUpdateNotebookForAllDescendants()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      move(firstChild, otherNotebook, true);

      makeMe.refresh(firstChild);
      makeMe.refresh(secondChild);
      makeMe.refresh(thirdLevel);

      assertThat(firstChild.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));
      assertThat(secondChild.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));
      assertThat(thirdLevel.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));

      assertThat(firstChild.getFolder(), notNullValue());
      assertThat(firstChild.getFolder().getName(), equalTo(otherNotebook.getNotebook().getName()));
      assertThat(
          firstChild.getFolder().getNotebook().getId(),
          equalTo(otherNotebook.getNotebook().getId()));
      assertThat(secondChild.getFolder(), notNullValue());
      assertThat(secondChild.getFolder().getName(), equalTo(firstChild.getTitle()));
      assertThat(thirdLevel.getFolder(), notNullValue());
      assertThat(thirdLevel.getFolder().getName(), equalTo(secondChild.getTitle()));
    }

    @Test
    void shouldUpdateNotebookForAllDescendantsIncludingRelationships()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      // Create a relationship note under secondChild
      Note targetNote = makeMe.aNote("targetNote").please();
      Note relationNote = makeMe.aRelation().between(secondChild, targetNote).please();

      move(secondChild, otherNotebook, true);

      makeMe.refresh(secondChild);
      makeMe.refresh(relationNote);

      assertThat(secondChild.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));
      assertThat(relationNote.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));

      assertThat(relationNote.getFolder(), notNullValue());
      assertThat(relationNote.getFolder().getName(), equalTo(secondChild.getTitle()));
    }
  }
}
