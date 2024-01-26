package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.json.LinkCreation;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
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
    return new RestLinkController(modelFactoryService, new TestabilitySettings(), userModel);
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
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller().show(link));
    }

    @Test
    void shouldNotBeAbleToSeeItIfICanReadOneNote() throws UnexpectedNoAccessRightException {
      makeMe.aBazaarNodebook(note1.getNotebook()).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller().show(link));
    }

    @Test
    void shouldBeAbleToSeeItIfICanReadBothNote() throws UnexpectedNoAccessRightException {
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
      note2 = makeMe.aNote("flower").creatorAndOwner(userModel).please();
      linkCreation.linkType = LinkType.APPLICATION;
      linkCreation.moveUnder = true;
    }

    @Test
    void createdSuccessfully()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      Note note3 = makeMe.aNote().creatorAndOwner(userModel).please();
      long beforeThingCount = makeMe.modelFactoryService.thingRepository.count();
      controller().linkNoteFinalize(note3, note2, linkCreation, makeMe.successfulBindingResult());
      long afterThingCount = makeMe.modelFactoryService.thingRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 2));
    }

    @Test
    void createdChildNoteSuccessfully()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      Note note3 = makeMe.aNote("flower tea").creatorAndOwner(userModel).please();
      controller().linkNoteFinalize(note3, note2, linkCreation, makeMe.successfulBindingResult());
      makeMe.refresh(note3);
      assertThat(note3.getChildren(), hasSize(0));
      assertThat(note3.getLinkChildren(), hasSize(1));
      assertThat(
          note3.getLinkChildren().get(0).getTopic(),
          equalTo("[flower tea] is an application of [flower]"));
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
          UnexpectedNoAccessRightException.class,
          () ->
              controller()
                  .linkNoteFinalize(note1, note2, linkCreation, makeMe.successfulBindingResult()));
    }

    @Test
    void shouldNotAllowMoveToOtherPeoplesNote() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () ->
              controller()
                  .linkNoteFinalize(note2, note1, linkCreation, makeMe.successfulBindingResult()));
    }
  }
}
