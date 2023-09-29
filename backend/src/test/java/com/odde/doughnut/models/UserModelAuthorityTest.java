package com.odde.doughnut.models;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class UserModelAuthorityTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  UserModel anotherUser;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    anotherUser = makeMe.aUser().toModelPlease();
  }

  @Nested
  class noteBelongsToACircle {
    CircleModel circleModel;
    Note note;

    @BeforeEach
    void setup() {
      circleModel = makeMe.aCircle().toModelPlease();
      note = makeMe.aNote().creator(makeMe.aUser().please()).inCircle(circleModel).please();
    }

    @Test
    void userCanNotAccessNotesBelongToCircle() {
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> userModel.assertAuthorization(note));
    }

    @Test
    void userCanAccessNotesBelongToCircleIfIsAMember() throws UnexpectedNoAccessRightException {
      makeMe.theCircle(circleModel).hasMember(userModel).please();
      userModel.assertAuthorization(note);
    }
  }

  @Nested
  class readAuthority {
    Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().please();
    }

    @Test
    void userCanNotAccessNotesBelongToCircle() {
      assertThrows(
          ResponseStatusException.class,
          () -> makeMe.aNullUserModel().assertReadAuthorization(note));
    }
  }
}
