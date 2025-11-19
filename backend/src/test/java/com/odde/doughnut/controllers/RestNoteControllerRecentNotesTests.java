package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.NoteMotionService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.services.search.NoteSearchService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteControllerRecentNotesTests {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  @Mock HttpClientAdapter httpClientAdapter;
  @Autowired NoteSearchService noteSearchService;
  @Autowired NoteMotionService noteMotionService;
  @Autowired com.odde.doughnut.services.NoteService noteService;
  private UserModel userModel;
  NoteController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller =
        new NoteController(
            modelFactoryService,
            userModel.getEntity(),
            makeMe.userService,
            makeMe.authorizationService,
            httpClientAdapter,
            testabilitySettings,
            noteMotionService,
            noteService);
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
            .creatorAndOwner(userModel)
            .createdAt(makeMe.aTimestamp().of(0, 0).please())
            .please();
    Note note2 =
        makeMe
            .aNote()
            .creatorAndOwner(userModel)
            .createdAt(makeMe.aTimestamp().of(0, 1).please())
            .please();

    var recentNotes = controller.getRecentNotes();
    assertThat(recentNotes, hasSize(2));
    assertThat(recentNotes.get(0).getId(), equalTo(note2.getId()));
    assertThat(recentNotes.get(1).getId(), equalTo(note1.getId()));
  }

  @Test
  void shouldNotAllowAccessWhenNotLoggedIn() {
    userModel = makeMe.aNullUserModelPlease();
    controller =
        new NoteController(
            modelFactoryService,
            null,
            makeMe.userService,
            makeMe.authorizationService,
            httpClientAdapter,
            testabilitySettings,
            noteMotionService,
            noteService);

    assertThrows(ResponseStatusException.class, () -> controller.getRecentNotes());
  }
}
