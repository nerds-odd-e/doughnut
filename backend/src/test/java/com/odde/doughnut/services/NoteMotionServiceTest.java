package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

  @Autowired MakeMe makeMe;
  Note topNote;
  Note firstChild;
  Note secondChild;

  @BeforeEach
  void setup() {
    topNote = makeMe.aHeadNote("topNote").please();
    firstChild = makeMe.aNote("firstChild").under(topNote).please();
    secondChild = makeMe.aNote("secondChild").under(topNote).please();
  }

  void move(Note subject, Note relativeNote, boolean asFirstChildOfNote)
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    noteMotionService.validate(subject, relativeNote, asFirstChildOfNote);
    noteMotionService.execute(subject, relativeNote, asFirstChildOfNote);
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
  void moveSecondBehindFirst() throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(secondChild, firstChild, false);
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
  }

  @Test
  void moveBothToTheEndInSequence()
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(firstChild, secondChild, false);
    move(secondChild, firstChild, false);
    assertOrder(firstChild, secondChild);
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
      otherNotebook = makeMe.aHeadNote("otherNotebook").please();
      firstChild = makeMe.aNote("firstChild").under(topNote).please();
      secondChild = makeMe.aNote("secondChild").under(firstChild).please();
      thirdLevel = makeMe.aNote("thirdLevel").under(secondChild).please();
    }

    @Test
    void shouldUpdateNotebookForAllDescendants()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      move(firstChild, otherNotebook, true);

      makeMe.refresh(firstChild);
      makeMe.refresh(secondChild);
      makeMe.refresh(thirdLevel);

      assertThat(firstChild.getNotebook(), equalTo(otherNotebook.getNotebook()));
      assertThat(secondChild.getNotebook(), equalTo(otherNotebook.getNotebook()));
      assertThat(thirdLevel.getNotebook(), equalTo(otherNotebook.getNotebook()));
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

      assertThat(secondChild.getNotebook(), equalTo(otherNotebook.getNotebook()));
      assertThat(relationNote.getNotebook(), equalTo(otherNotebook.getNotebook()));
    }
  }
}
