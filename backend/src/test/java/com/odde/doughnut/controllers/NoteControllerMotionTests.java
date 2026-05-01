package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.NoteChildContainerFolderService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerMotionTests extends ControllerTestBase {

  @MockitoBean HttpClientAdapter httpClientAdapter;
  @Autowired NoteController controller;
  @Autowired NoteChildContainerFolderService noteChildContainerFolderService;
  Note subject;

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

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    subject = makeMe.aNote("subject").creatorAndOwner(currentUser.getUser()).please();
  }

  @Nested
  class NoteWithParent {
    Note parent;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote("parent").creatorAndOwner(currentUser.getUser()).please();
      subject = makeMe.theNote(subject).under(parent).please();
    }

    @Test
    void shouldCheckAccessRight() {
      Note note = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveAfter(note, parent, "asFirstChild"));
    }

    @Test
    void shouldNotMoveUpIfTheNoteIsAlreadyTheFirstChild() {
      assertThrows(
          MovementNotPossibleException.class,
          () -> controller.moveAfter(subject, parent, "asFirstChild"));
    }

    @Nested
    class NoteWithSiblings {
      Note previousOlder;
      Note previousYounger;

      @BeforeEach
      void setup() {
        previousOlder = makeMe.aNote("previous older sibling").asFirstChildOf(parent).please();
        previousYounger = makeMe.aNote("previous younger sibling").after(subject).please();
        // make sure the children are in the right order, by reloading from db
        makeMe.refresh(parent);
      }

      @Test
      void makeSureTheInitialStateIsAsExpected() {
        assertThat(previousOlder.getSiblingOrder(), lessThan(subject.getSiblingOrder()));
        assertThat(subject.getSiblingOrder(), lessThan(previousYounger.getSiblingOrder()));
      }

      @Test
      void shouldMoveUpToAsFirstChild()
          throws UnexpectedNoAccessRightException,
              CyclicLinkDetectedException,
              MovementNotPossibleException {
        var noteRealms = controller.moveAfter(subject, parent, "asFirstChild");
        assertThat(subject.getSiblingOrder(), lessThan(previousOlder.getSiblingOrder()));
        assertThat(noteRealms.size(), equalTo(1));
        assertThat(noteRealms.get(0).getNote(), equalTo(parent));
      }

      @Test
      void shouldMoveUpToAsSecondChild()
          throws UnexpectedNoAccessRightException,
              CyclicLinkDetectedException,
              MovementNotPossibleException {
        var noteRealms = controller.moveAfter(subject, previousYounger, "");
        assertThat(previousOlder.getSiblingOrder(), lessThan(previousYounger.getSiblingOrder()));
        assertThat(previousYounger.getSiblingOrder(), lessThan(subject.getSiblingOrder()));
        assertThat(noteRealms.get(0).getNote(), equalTo(parent));
      }

      @Test
      void shouldMoveToAfterParent()
          throws UnexpectedNoAccessRightException,
              CyclicLinkDetectedException,
              MovementNotPossibleException {
        Note grand = makeMe.aNote("grand").under(subject).please();
        var noteRealms = controller.moveAfter(grand, subject, "");
        assertThat(grand.getParent(), equalTo(parent));
        assertThat(noteRealms.size(), equalTo(2));
      }

      @Test
      void shouldUpdateTargetNoteChildrenWhenMovingAsFirstChild()
          throws UnexpectedNoAccessRightException,
              CyclicLinkDetectedException,
              MovementNotPossibleException {
        // Move subject as first child of previousYounger
        controller.moveAfter(subject, previousYounger, "asFirstChild");

        assertThat(previousYounger.getChildren(), hasItem(subject));
      }
    }
  }

  @Nested
  class FolderAlignmentOnMove {
    @Test
    void moveAsFirstChildOfSibling_assignsChildContainerFolder()
        throws UnexpectedNoAccessRightException,
            CyclicLinkDetectedException,
            MovementNotPossibleException {
      currentUser.setUser(makeMe.aUser().please());
      Note root = makeMe.aRootNote("top").creatorAndOwner(currentUser.getUser()).please();
      Note section = makeMe.aNote("Section").under(root).please();
      Note leaf = makeMe.aNote("leaf").under(section).please();
      Note siblingContainer = makeMe.aNote("container").under(section).please();
      makeMe.entityPersister.flush();
      controller.moveAfter(leaf, siblingContainer, "asFirstChild");
      makeMe.refresh(leaf);
      assertThat(leaf.getFolder(), notNullValue());
      assertThat(leaf.getFolder().getName(), equalTo(siblingContainer.getTitle()));
    }

    @Test
    void moveIntoFolderWhereTitlesCollide_updatesParentAndNotebookScope()
        throws UnexpectedNoAccessRightException,
            CyclicLinkDetectedException,
            MovementNotPossibleException {
      currentUser.setUser(makeMe.aUser().please());
      Note root = makeMe.aRootNote("top").creatorAndOwner(currentUser.getUser()).please();
      Note section = makeMe.aNote("Section").under(root).please();
      makeMe.aNote("dup").under(section).please();
      Note secondDup = makeMe.aNote("dup").under(section).please();
      Note otherRoot = makeMe.aRootNote("otherNb").creatorAndOwner(currentUser.getUser()).please();
      Note mover = makeMe.aNote("dup").under(otherRoot).please();
      makeMe.entityPersister.flush();
      alignFoldersForTestSubtree(root);
      alignFoldersForTestSubtree(otherRoot);
      makeMe.entityPersister.flush();

      controller.moveAfter(mover, secondDup, "");
      makeMe.refresh(mover);
      assertThat(mover.getParent(), equalTo(section));
      assertThat(mover.getNotebook().getId(), equalTo(root.getNotebook().getId()));
    }
  }
}
