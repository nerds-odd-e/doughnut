package com.odde.doughnut.models;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserModelAuthorityTest {
  @Autowired MakeMe makeMe;
  @Autowired AuthorizationService authorizationService;
  User user;
  User anotherUser;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    anotherUser = makeMe.aUser().please();
  }

  @Nested
  class noteBelongsToACircle {
    Circle circle;
    Note note;

    @BeforeEach
    void setup() {
      circle = makeMe.aCircle().please();
      note = makeMe.aNote().creator(makeMe.aUser().please()).inCircle(circle).please();
    }

    @Test
    void userCanNotAccessNotesBelongToCircle() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> authorizationService.assertAuthorization(user, note));
    }

    @Test
    void userCanAccessNotesBelongToCircleIfIsAMember() throws UnexpectedNoAccessRightException {
      makeMe.theCircle(circle).hasMember(user).please();
      authorizationService.assertAuthorization(user, note);
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
          () -> authorizationService.assertReadAuthorization(null, note));
    }
  }
}
