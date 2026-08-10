package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.LearningSessionRequestResponse;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LearningSessionRequestTests extends LearningSessionControllerTestBase {

  @Test
  void returnsRequestMarkdownFromDueTrackersWithoutCreatingSession()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);
    long sessionsBefore = learningSessionRepository.count();

    LearningSessionRequestResponse response = controller.request(notebook.getId(), "Asia/Shanghai");

    assertThat(response.getRequestMarkdown(), containsString("# Learning Session Request"));
    assertThat(response.getRequestMarkdown(), containsString("### Hola"));
    assertThat(response.getRequestMarkdown(), containsString("### Gracias"));
    assertThat(learningSessionRepository.count(), equalTo(sessionsBefore));
  }

  @Test
  void badRequestWhenNoDueCommissionedTrackers() {
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Empty notebook").please();
    makeMe.aNote().notebook(notebook).title("Lonely").content("Alone").please();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.request(notebook.getId(), "Asia/Shanghai"));
    assertThat(ex.getStatusCode().value(), equalTo(400));
  }

  @Test
  void notebookNotFound() {
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> controller.request(999_999, "Asia/Shanghai"));
    assertThat(ex.getStatusCode().value(), equalTo(404));
  }

  @Test
  void unauthorizedNotebook() {
    Notebook foreignNotebook = makeMe.aNotebook().please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.request(foreignNotebook.getId(), "Asia/Shanghai"));
  }
}
