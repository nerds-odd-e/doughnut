package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
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
class RestNoteControllerMotionTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  @Mock HttpClientAdapter httpClientAdapter;
  private UserModel userModel;
  RestNoteController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  Note subject;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller =
        new RestNoteController(
            modelFactoryService, userModel, httpClientAdapter, testabilitySettings);
    subject = makeMe.aNote("subject").please();
  }

  @Test
  void shouldNotMoveUpIfThereIsNoPreviousSibling() {
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.moveUp(subject));
  }

  @Nested
  class NoteWithParent {
    Note parent;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote("parent").creatorAndOwner(userModel).please();
      subject = makeMe.theNote(subject).under(parent).please();
    }

    @Test
    void shouldNotMoveUpIfThereIsNoPreviousSibling() {
      assertThrows(MovementNotPossibleException.class, () -> controller.moveUp(subject));
    }

    @Nested
    class NoteWithSiblings {
      Note previousOlder;
      Note youngerSibling;

      @BeforeEach
      void setup() {
        makeMe.refresh(parent);
        previousOlder = makeMe.aNote("previous older sibling").asFirstChildOf(parent).please();
        //        youngerSibling = makeMe.aNote().after(note).please();
        makeMe.refresh(parent);
      }

      @Test
      void shouldMoveUpWhenThereIsOneOlderSibling()
          throws UnexpectedNoAccessRightException,
              CyclicLinkDetectedException,
              MovementNotPossibleException {
        assertThat(previousOlder.getSiblingOrder(), lessThan(subject.getSiblingOrder()));
        var noteRealm = controller.moveUp(subject);
        assertThat(noteRealm.getId(), equalTo(parent.getId()));
        assertThat(subject.getSiblingOrder(), lessThan(previousOlder.getSiblingOrder()));
      }
    }
  }
}
