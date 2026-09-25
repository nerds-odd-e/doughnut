package com.odde.donut.services;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Circle;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.SpringTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AuthorizationServiceTest extends SpringTestBase {
  User user;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
  }

  @Nested
  class noteBelongsToACircle {
    Circle circle;
    Note note;

    @BeforeEach
    void setup() {
      circle = makeMe.aCircle().please();
      note = makeMe.aNote().inCircle(circle).please();
    }

    @Test
    void nonMemberCannotAccess() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> authorizationService.assertAuthorization(user, note));
    }

    @Test
    void memberCanAccess() throws UnexpectedNoAccessRightException {
      makeMe.theCircle(circle).hasMember(user).please();
      authorizationService.assertAuthorization(user, note);
    }
  }

  @Nested
  class readAuthority {
    @Test
    void anonymousUserCannotRead() {
      Note note = makeMe.aNote().please();
      assertThrows(
          ResponseStatusException.class,
          () -> authorizationService.assertReadAuthorization((User) null, note));
    }
  }
}
