package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteRecallSetting;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerUpdateNoteRecallSettingTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired UserService userService;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldPutNoteBackToAssimilationListWhenRememberSpellingIsAddedLater()
      throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aMemoryTrackerFor(source).please();

    NoteRecallSetting noteRecallSetting = new NoteRecallSetting();
    noteRecallSetting.setRememberSpelling(true);
    controller.updateNoteRecallSetting(source, noteRecallSetting);

    assertThat(
        userService.getUnassimilatedNotes(currentUser.getUser()).toList(),
        hasItem(hasProperty("id", equalTo(source.getId()))));
  }
}
