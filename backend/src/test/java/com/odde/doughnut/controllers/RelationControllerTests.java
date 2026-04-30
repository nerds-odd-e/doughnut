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
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.RelationshipNoteMarkdownFormatter;
import com.odde.doughnut.services.RelationshipNoteTitleFormatter;
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
        throws BindException,
            UnexpectedNoAccessRightException,
            CyclicLinkDetectedException,
            MovementNotPossibleException {
      Note note3 = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      noteMoveDTO.asFirstChild = false;
      var result = controller.moveNote(note3, note2, noteMoveDTO, makeMe.successfulBindingResult());
      assertThat(result, hasSize(2));
    }

    @Test
    void shouldRejectMovingAsFirstChildWhenSubjectIsAlreadyFirstChildOfTarget() {
      Note parentNote = makeMe.aNote("parent").creatorAndOwner(currentUser.getUser()).please();
      Note onlyChild =
          makeMe.aNote("only").creatorAndOwner(currentUser.getUser()).under(parentNote).please();
      noteMoveDTO.asFirstChild = true;
      assertThrows(
          MovementNotPossibleException.class,
          () ->
              controller.moveNote(
                  onlyChild, parentNote, noteMoveDTO, makeMe.successfulBindingResult()));
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

    @Test
    void relationshipNoteHasDerivedTitle()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      Note source = makeMe.aNote("Tool").creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote("Task").creatorAndOwner(currentUser.getUser()).please();
      relationshipCreation.relationType = RelationType.APPLICATION;
      var result =
          controller.addRelationshipFinalize(
              source, target, relationshipCreation, makeMe.successfulBindingResult());
      assertThat(result, hasSize(3));
      String expected =
          RelationshipNoteTitleFormatter.format(
              source.getTitle(), RelationType.APPLICATION.label, target.getTitle());
      assertThat(result.getFirst().getNote().getTitle(), equalTo(expected));
      String expectedDetails =
          RelationshipNoteMarkdownFormatter.format(
              RelationType.APPLICATION, source.getTitle(), target.getTitle(), null);
      assertThat(result.getFirst().getNote().getDetails(), equalTo(expectedDetails));
    }
  }

  @Nested
  class UpdateRelationshipTest {
    RelationshipCreation relationshipCreation = new RelationshipCreation();

    @Test
    void updatesRelationshipNoteTitleWhenRelationTypeChanges()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      Note source = makeMe.aNote("Moon").creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote("Earth").creatorAndOwner(currentUser.getUser()).please();
      relationshipCreation.relationType = RelationType.PART;
      var created =
          controller.addRelationshipFinalize(
              source, target, relationshipCreation, makeMe.successfulBindingResult());
      Note relationNote = created.getFirst().getNote();

      relationshipCreation.relationType = RelationType.SPECIALIZE;
      var after = controller.updateRelationship(relationNote, relationshipCreation);

      String expected =
          RelationshipNoteTitleFormatter.format(
              source.getTitle(), RelationType.SPECIALIZE.label, target.getTitle());
      assertThat(after.getFirst().getNote().getTitle(), equalTo(expected));
    }
  }
}
