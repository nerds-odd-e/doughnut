package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;

import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
class IndexControllerTests {

  @Autowired private NoteRepository noteRepository;
  @Autowired private UserRepository userRepository;

  @Test
  void visitWithNoUserSession() {
    Model model = mock(Model.class);
    IndexController controller = new IndexController(noteRepository, userRepository);

    String home = controller.home(null, model);
    assertEquals("login", home);
  }

  @Test
  void visitWithUserSessionButNoSuchARegisteredUserYet() {
    IndexController controller = new IndexController(noteRepository, userRepository);
    Principal user = (UserPrincipal) () -> "1234567";
    Model model = mock(Model.class);
    assertEquals("register", controller.home(user, model));
  }

  @Test
  void shouldBeRedirectToLandingPageWhenUserIsNotLogIn() {
    IndexController controller = new IndexController(noteRepository, userRepository);
    Model model = mock(Model.class);
    assertEquals("redirect:/", controller.notes(null, model));
  }

  @Test
  void shouldProceedToNotePageWhenUserIsLogIn() {
    IndexController controller = new IndexController(noteRepository, userRepository);
    Principal user = (UserPrincipal) () -> "1234567";
    Model model = mock(Model.class);
    assertEquals("note", controller.notes(user, model));
  }

  @Test
  void shouldProceedToReviewPage() {
    User user = mock(User.class);
    when(user.getNotesInDescendingOrder()).thenReturn(new ArrayList<>());
    IndexController controller = new IndexController(noteRepository, createMockUserRepository(user));
    Principal login = (UserPrincipal) () -> "1234567";
    Model model = mock(Model.class);
    assertEquals("review", controller.review(login, model));
  }

  private UserRepository createMockUserRepository(User user) {
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findByExternalIdentifier("1234567")).thenReturn(user);
    return userRepository;
  }

}
