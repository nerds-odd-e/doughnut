package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.SpellingVerificationResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerVerifySpellingTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private SpellingVerificationResult verify(Note note, String answer)
      throws UnexpectedNoAccessRightException {
    AnswerSpellingDTO dto = new AnswerSpellingDTO();
    dto.setSpellingAnswer(answer);
    return controller.verifySpelling(note, dto);
  }

  @ParameterizedTest
  @CsvSource({
    "sedition, sedition, true",
    "colour／color, colour／color, true",
    "colour／color, color, false",
    "sedition, wrong answer, false",
  })
  void verifiesLiteralTitleSpelling(String title, String answer, boolean expected)
      throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().title(title).notebookOwnedBy(currentUser.getUser()).please();
    assertThat(verify(note, answer).correct(), is(expected));
  }

  @Test
  void returnsCorrectWhenFrontmatterAliasMatches() throws UnexpectedNoAccessRightException {
    Note note =
        makeMe
            .aNote()
            .title("colour")
            .aliases("color")
            .notebookOwnedBy(currentUser.getUser())
            .please();
    assertThat(verify(note, "color").correct(), is(true));
  }
}
