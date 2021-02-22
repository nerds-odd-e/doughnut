package com.odde.doughnut.controllers;

import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;

import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
class IndexControllerTests {

  @Autowired private NoteRepository noteRepository;
  @Autowired private UserRepository userRepository;
  @Mock Model model;
  private IndexController controller;
  private MakeMe makeMe;

  @BeforeEach
  void setupController() {
    makeMe = new MakeMe(null);
    controller = new IndexController(noteRepository, userRepository);
  }

  @Test
  void visitWithNoUserSession() {
    assertEquals("login", controller.home(null, model));
  }

  @Test
  void visitWithUserSessionButNoSuchARegisteredUserYet() {
    Principal principal = (UserPrincipal) () -> "1234567";
    assertEquals("register", controller.home(principal, model));
  }

  @Test
  void visitWithUserSessionAndTheUserExists() {
    User user = makeMe.aUser().please(userRepository);
    Principal principal = (UserPrincipal) user::getExternalIdentifier;
    assertEquals("index", controller.home(principal, model));
  }
}
