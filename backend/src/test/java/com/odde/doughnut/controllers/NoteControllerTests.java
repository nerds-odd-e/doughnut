package com.odde.doughnut.controllers;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
class NoteControllerTests {
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private NoteRepository noteRepository;

  @Test
  void shouldProceedToNotePageWhenUserIsLogIn() {
    NoteController controller = new NoteController(userRepository, noteRepository);
    Principal user = (UserPrincipal) () -> "1234567";
    Model model = mock(Model.class);
    assertEquals("note", controller.notes(model));
  }

}
