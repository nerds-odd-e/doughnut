package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteMotion;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
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
public class NoteMotionModelTest {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  Note topNote;
  Note firstChild;
  Note secondChild;

  @BeforeEach
  void setup() {
    topNote = makeMe.aHeadNote("topNote").please();
    firstChild = makeMe.aNote("firstChild").under(topNote).please();
    secondChild = makeMe.aNote("secondChild").under(topNote).please();
    makeMe.refresh(topNote);
    makeMe.refresh(firstChild);
    makeMe.refresh(secondChild);
  }

  void move(Note subject, Note relativeNote, boolean asFirstChildOfNote)
      throws CyclicLinkDetectedException {
    NoteMotion motion = new NoteMotion(relativeNote, asFirstChildOfNote);
    NoteMotionModel noteMotionModel = modelFactoryService.toNoteMotionModel(motion, subject);
    noteMotionModel.execute();
  }

  @Test
  void moveBehind() throws CyclicLinkDetectedException {
    move(firstChild, secondChild, false);
    assertOrder(secondChild, firstChild);
  }

  private void assertOrder(Note note1, Note note2) {
    Note parentNote = note1.getParent();
    makeMe.refresh(parentNote);
    assertThat(parentNote.getChildren(), containsInRelativeOrder(note1, note2));
  }

  @Test
  void moveSecondBehindFirst() throws CyclicLinkDetectedException {
    move(secondChild, firstChild, false);
    assertOrder(firstChild, secondChild);
  }

  @Test
  void moveSecondToBeTheFirstSibling() throws CyclicLinkDetectedException {
    move(secondChild, topNote, true);
    assertOrder(secondChild, firstChild);
  }

  @Test
  void moveUnder() throws CyclicLinkDetectedException {
    move(firstChild, secondChild, true);
    assertThat(firstChild.getParent(), equalTo(secondChild));
  }

  @Test
  void moveBothToTheEndInSequence() throws CyclicLinkDetectedException {
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
    void moveAfterNoteOfDifferentLevel() throws CyclicLinkDetectedException {
      move(secondChild, thirdLevel, false);
      assertThat(secondChild.getParent(), equalTo(firstChild));
    }

    @Test
    void moveToOwnDescendentIsNotAllowed() {
      assertThrows(CyclicLinkDetectedException.class, () -> move(topNote, thirdLevel, false));
    }

    @Test
    void moveWithOwnChild() throws CyclicLinkDetectedException {
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
    void moveBetweenSecondAndThird() throws CyclicLinkDetectedException {
      move(firstChild, secondChild, false);
      assertOrder(secondChild, firstChild);
      assertOrder(firstChild, thirdChild);
    }
  }
}
