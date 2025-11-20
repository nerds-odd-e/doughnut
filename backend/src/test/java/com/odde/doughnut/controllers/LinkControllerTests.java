package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.LinkCreation;
import com.odde.doughnut.controllers.dto.NoteMoveDTO;
import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.NoteMotionService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;

class LinkControllerTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;

  @Autowired NoteMotionService noteMotionService;

  @Autowired NoteService noteService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  LinkController controller() {
    return new LinkController(
        makeMe.entityPersister,
        noteService,
        new TestabilitySettings(),
        noteMotionService,
        authorizationService);
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
      var result =
          controller().moveNote(note3, note2, noteMoveDTO, makeMe.successfulBindingResult());
      assertThat(result, hasSize(2));
    }

    @Test
    void shouldNotAllowMoveOtherPeoplesNote() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller().moveNote(note1, note2, noteMoveDTO, makeMe.successfulBindingResult()));
    }

    @Test
    void shouldNotAllowMoveToOtherPeoplesNote() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller().moveNote(note2, note1, noteMoveDTO, makeMe.successfulBindingResult()));
    }
  }

  @Nested
  class CreateLinkTest {
    User anotherUser;
    Note note1;
    Note note2;
    LinkCreation linkCreation = new LinkCreation();

    @BeforeEach
    void setup() {
      anotherUser = makeMe.aUser().please();
      note1 = makeMe.aNote().creatorAndOwner(anotherUser).please();
      note2 = makeMe.aNote("flower").creatorAndOwner(currentUser.getUser()).please();
      linkCreation.linkType = LinkType.APPLICATION;
    }

    @Test
    void createdSuccessfully()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      Note note3 = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      long beforeThingCount = noteRepository.count();
      controller().linkNoteFinalize(note3, note2, linkCreation, makeMe.successfulBindingResult());
      long afterThingCount = noteRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }
  }
}
