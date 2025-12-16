package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteMoveDTO;
import com.odde.doughnut.controllers.dto.RelationshipCreation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;

class RelationControllerTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired RelationController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class MoveNoteTest {
    User anotherUser;
    Note note1;
    Note note2;
    NoteMoveDTO noteMoveDTO = new NoteMoveDTO();

    @BeforeEach
    void setup() {
      anotherUser = makeMe.aUser().please();
      note1 = makeMe.aNote().creatorAndOwner(anotherUser).please();
      note2 = makeMe.aNote("flower").creatorAndOwner(currentUser.getUser()).please();
      noteMoveDTO.asFirstChild = false;
    }

    @Test
    void moveNoteSuccessfully()
        throws BindException, UnexpectedNoAccessRightException, CyclicLinkDetectedException {
      Note note3 = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      noteMoveDTO.asFirstChild = false;
      var result = controller.moveNote(note3, note2, noteMoveDTO, makeMe.successfulBindingResult());
      assertThat(result, hasSize(2));
    }

    @Test
    void shouldNotAllowMoveOtherPeoplesNote() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNote(note1, note2, noteMoveDTO, makeMe.successfulBindingResult()));
    }

    @Test
    void shouldNotAllowMoveToOtherPeoplesNote() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNote(note2, note1, noteMoveDTO, makeMe.successfulBindingResult()));
    }
  }

  @Nested
  class CreateRelationshipTest {
    User anotherUser;
    Note note1;
    Note note2;
    RelationshipCreation relationshipCreation = new RelationshipCreation();

    @BeforeEach
    void setup() {
      anotherUser = makeMe.aUser().please();
      note1 = makeMe.aNote().creatorAndOwner(anotherUser).please();
      note2 = makeMe.aNote("flower").creatorAndOwner(currentUser.getUser()).please();
      relationshipCreation.relationType = RelationType.APPLICATION;
    }

    @Test
    void createdSuccessfully()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      Note note3 = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      long beforeThingCount = noteRepository.count();
      controller.addRelationshipFinalize(
          note3, note2, relationshipCreation, makeMe.successfulBindingResult());
      long afterThingCount = noteRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }
  }
}
