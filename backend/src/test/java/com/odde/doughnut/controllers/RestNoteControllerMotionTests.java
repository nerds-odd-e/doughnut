package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteMotionService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.services.search.NoteSearchService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteControllerMotionTests {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired AuthorizationService authorizationService;

  @Autowired MakeMe makeMe;
  @Mock HttpClientAdapter httpClientAdapter;
  @Autowired NoteSearchService noteSearchService;
  @Autowired NoteMotionService noteMotionService;
  @Autowired com.odde.doughnut.services.NoteService noteService;
  private CurrentUser userModel;
  NoteController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  Note subject;

  @BeforeEach
  void setup() {
    userModel = new CurrentUser(makeMe.aUser().toModelPlease());
    controller =
        new NoteController(
            modelFactoryService,
            userModel,
            httpClientAdapter,
            testabilitySettings,
            noteMotionService,
            noteService,
            authorizationService);
    subject = makeMe.aNote("subject").creatorAndOwner(userModel.getUserModel()).please();
  }

  @Nested
  class NoteWithParent {
    Note parent;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote("parent").creatorAndOwner(userModel.getUserModel()).please();
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
}
