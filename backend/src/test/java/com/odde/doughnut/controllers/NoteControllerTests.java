package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
class NoteControllerTests {
  @Autowired private NoteRepository noteRepository;
  @Autowired private UserRepository userRepository;
  private MakeMe makeMe;
  private User user;
  private NoteRestController noteController;
  ExtendedModelMap model = new ExtendedModelMap();
  NoteController controller;

  @BeforeEach
  void setup() {
    makeMe = new MakeMe();
    user = makeMe.aUser().please(userRepository);
    controller = new NoteController(new TestCurrentUser(user), noteRepository);
  }

  @Test
  void shouldProceedToNotePageWhenUserIsLogIn() {
    assertEquals("note", controller.notes(model));
  }

  @Test
  void shouldUseAllMyNotesTemplate() {
    assertEquals("all_my_notes", controller.all_my_notes(model));
  }

  @Test
  void shouldReturnAllParentlessNoteIfNoNoteIdGiven() {
    controller.all_my_notes(model);
    assertThat(model.getAttribute("note"), is(nullValue()));
  }

}
