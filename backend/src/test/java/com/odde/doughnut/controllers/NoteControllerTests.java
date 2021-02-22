package com.odde.doughnut.controllers;

import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.testability.DBCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
class NoteControllerTests {
  @Autowired private NoteRepository noteRepository;
  @Mock Model model;

  @Test
  void shouldProceedToNotePageWhenUserIsLogIn() {
    NoteController controller = new NoteController(null, noteRepository);
    assertEquals("note", controller.notes(model));
  }
}
