package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.LinkCreation;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestLinkControllerTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
  }

  RestLinkController controller() {
    return new RestLinkController(modelFactoryService, makeMe.aTimestamp().please(), userModel);
  }

  @Nested
  class showLinkTest {
    User otherUser;
    Note note1;
    Note note2;
    Link link;

    @BeforeEach
    void setup() {
      otherUser = makeMe.aUser().please();
      note1 = makeMe.aNote().creatorAndOwner(otherUser).please();
      note2 = makeMe.aNote().creatorAndOwner(otherUser).linkTo(note1).please();
      link = note2.getLinks().get(0);
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      assertThrows(NoAccessRightException.class, () -> controller().show(link));
    }

    @Test
    void shouldNotBeAbleToSeeItIfICanReadOneNote() throws NoAccessRightException {
      makeMe.aBazaarNodebook(note1.getNotebook()).please();
      assertThrows(NoAccessRightException.class, () -> controller().show(link));
    }

    @Test
    void shouldBeAbleToSeeItIfICanReadBothNote() throws NoAccessRightException {
      makeMe.aBazaarNodebook(note1.getNotebook()).please();
      makeMe.aBazaarNodebook(note2.getNotebook()).please();
      Link linkViewedByUser = controller().show(link);
      assertThat(linkViewedByUser.getId(), equalTo(link.getId()));
    }
  }

  @Nested
  class MoveNoteTest {
    User anotherUser;
    Note note1;
    Note note2;
    LinkCreation linkCreation = new LinkCreation();

    @BeforeEach
    void setup() {
      anotherUser = makeMe.aUser().please();
      note1 = makeMe.aNote().creatorAndOwner(anotherUser).please();
      note2 = makeMe.aNote().creatorAndOwner(userModel).please();
      linkCreation.linkType = LinkType.APPLICATION;
      linkCreation.moveUnder = true;
    }

    @Test
    void createdSuccessfully()
        throws CyclicLinkDetectedException, BindException, NoAccessRightException {
      Note note3 = makeMe.aNote().creatorAndOwner(userModel).please();
      long beforeThingCount = makeMe.modelFactoryService.thingRepository.count();
      controller().linkNoteFinalize(note3, note2, linkCreation, makeMe.successfulBindingResult());
      long afterThingCount = makeMe.modelFactoryService.thingRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }

    @Test
    void userNotLoggedIn() {
      userModel = makeMe.aNullUserModel();
      assertThrows(
          ResponseStatusException.class,
          () ->
              controller()
                  .linkNoteFinalize(note1, note2, linkCreation, makeMe.successfulBindingResult()));
    }

    @Test
    void linkTypeIsEmpty() {
      assertThrows(
          BindException.class,
          () ->
              controller()
                  .linkNoteFinalize(note1, note2, linkCreation, makeMe.failedBindingResult()));
    }

    @Test
    void shouldNotAllowMoveOtherPeoplesNote() {
      assertThrows(
          NoAccessRightException.class,
          () ->
              controller()
                  .linkNoteFinalize(note1, note2, linkCreation, makeMe.successfulBindingResult()));
    }

    @Test
    void shouldNotAllowMoveToOtherPeoplesNote() {
      assertThrows(
          NoAccessRightException.class,
          () ->
              controller()
                  .linkNoteFinalize(note2, note1, linkCreation, makeMe.successfulBindingResult()));
    }
  }
}
