package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.IndexController;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.testability.DBCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;

import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
class IndexControllerTests {

  @Autowired private NoteRepository noteRepository;

  @Test
  void visitWithNoUserSession() {
    Model model = mock(Model.class);
    IndexController controller = new IndexController(noteRepository);

    String home = controller.home(null, model);
    assertEquals("login", home);
  }

  @Test
  void visitWithUserSessionButNoSuchARegisteredUserYet() {
    IndexController controller = new IndexController(noteRepository);
    Principal user = new UserPrincipal() {
      @Override
      public String getName() {
        return "Tom";
      }
    };
    Model model = mock(Model.class);
    assertEquals("index", controller.home(user, model));
  }

}
