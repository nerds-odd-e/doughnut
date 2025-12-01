package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class NoteControllerRecentNotesTests extends ControllerTestBase {
  @MockitoBean HttpClientAdapter httpClientAdapter;
  @Autowired NoteController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldReturnEmptyListWhenNoNotes() throws UnexpectedNoAccessRightException {
    assertThat(controller.getRecentNotes(), empty());
  }

  @Test
  void shouldReturnRecentNotes() throws UnexpectedNoAccessRightException {
    Note note1 =
        makeMe
            .aNote()
            .creatorAndOwner(currentUser.getUser())
            .createdAt(makeMe.aTimestamp().of(0, 0).please())
            .please();
    Note note2 =
        makeMe
            .aNote()
            .creatorAndOwner(currentUser.getUser())
            .createdAt(makeMe.aTimestamp().of(0, 1).please())
            .please();

    var recentNotes = controller.getRecentNotes();
    assertThat(recentNotes, hasSize(2));
    assertThat(recentNotes.get(0).getNoteTopology().getId(), equalTo(note2.getId()));
    assertThat(recentNotes.get(1).getNoteTopology().getId(), equalTo(note1.getId()));
  }

  @Test
  void shouldNotAllowAccessWhenNotLoggedIn() {
    currentUser.setUser(null);
    assertThrows(ResponseStatusException.class, () -> controller.getRecentNotes());
  }
}
