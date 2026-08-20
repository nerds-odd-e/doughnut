package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.LearningSessionRequestResponse;
import com.odde.doughnut.entities.Grade;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.LearningSessionReportParser;
import com.odde.doughnut.services.focusContext.FocusContextConstants;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LearningSessionRequestTests extends LearningSessionControllerTestBase {

  @Test
  void returnsRequestMarkdownFromDueTrackersWithoutWritingRecallLogs()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);
    long logsBefore = recallLogRepository.count();

    LearningSessionRequestResponse response = controller.request(notebook.getId(), "Asia/Shanghai");

    String markdown = response.getRequestMarkdown();
    assertThat(markdown, containsString("# Learning Session Request"));
    assertThat(markdown, containsString("<instructions>"));
    assertThat(markdown, containsString("</instructions>"));
    assertThat(
        markdown,
        containsString("You are the tutor to help the learner to study Spanish conversation."));
    assertThat(
        markdown,
        containsString("Wait for the learner's instruction before starting the learning session."));
    assertThat(markdown, not(containsString("Focus on conversational phrases")));
    assertThat(markdown, containsString("<session_item_titles>"));
    assertThat(markdown, containsString("- Hola\n"));
    assertThat(markdown, containsString("- Gracias\n"));
    assertThat(markdown, containsString("</session_item_titles>"));
    assertThat(markdown, containsString("<session_items>"));
    assertThat(markdown, containsString("### Hola"));
    assertThat(markdown, containsString("### Gracias"));
    assertThat(markdown, not(containsString("Expected learning content:")));
    assertThat(markdown, containsString("- Tutoring status: not yet tutored"));
    assertThat(markdown, containsString("Hello"));
    assertThat(markdown, containsString("Thank you"));
    assertThat(markdown, containsString(FocusContextConstants.FOCUS_CONTEXT_OPEN_MARKER));
    assertThat(markdown, containsString(FocusContextConstants.FOCUS_NOTE_OPEN_MARKER));
    assertThat(markdown, containsString("```doughnut-note-md"));
    assertThat(markdown, containsString("Title: Hola"));
    assertThat(markdown, containsString("Title: Gracias"));
    assertThat(markdown, containsString("</session_items>"));
    assertThat(markdown, containsString("<how_to_report>"));
    assertThat(
        markdown,
        containsString(
            "Teach the session items above, then return a Learning Session Report giving one"));
    assertThat(markdown, containsString("Grade from 1 to 4 per item"));
    assertThat(markdown, containsString("Example of how to provide feedback:"));
    assertThat(
        markdown,
        containsString(
            "# Learning Session Report\n\n"
                + LearningSessionReportParser.SESSION_ITEM_GRADES_OPEN_TAG
                + "\nHola: 4\nGracias: 1\n"
                + LearningSessionReportParser.SESSION_ITEM_GRADES_CLOSE_TAG));
    assertThat(
        markdown,
        containsString(
            "Only grade session items that were actually taught in this session. Do not list"));
    assertThat(markdown, containsString("items that were not taught in the session."));
    assertThat(markdown, containsString("</how_to_report>"));
    assertThat(markdown, containsString("- 4 — mastered the session item with full fluency"));
    assertThat(markdown, containsString("- 3 — mastered the session item with fluency"));
    assertThat(
        markdown,
        containsString(
            "- 2 — mastered the session item but not fluent, or needed a reminder then showed"
                + " mastery"));
    assertThat(
        markdown,
        containsString(
            "- 1 — needed several reminders, or could not reach the session item even with help"));
    assertThat(recallLogRepository.count(), equalTo(logsBefore));
  }

  @Test
  void requestMarkdownIncludesNotebookQuestionGenerationInstruction()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);
    makeMe
        .theNotebook(notebook)
        .readmeContent(
            "---\nquestion_generation_instruction: Focus on conversational phrases.\n---\n")
        .please();

    LearningSessionRequestResponse response = controller.request(notebook.getId(), "Asia/Shanghai");

    assertThat(
        response.getRequestMarkdown(),
        containsString(
            "<instructions>\nYou are the tutor to help the learner to study Spanish"
                + " conversation.\n\nFocus on conversational phrases.\n\nWait for the learner's"
                + " instruction before starting the learning session.\n</instructions>"));
  }

  @Test
  void requestMarkdownReflectsPriorRecordedFeedbackPerTracker()
      throws UnexpectedNoAccessRightException {
    Timestamp priorSessionAt = makeMe.aTimestamp().of(5, 10).fromShanghai().please();
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).fromShanghai().please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    makeMe
        .aRecallLogFor(fixture.holaTracker())
        .grade(Grade.GOOD)
        .recordedAt(priorSessionAt)
        .please();

    LearningSessionRequestResponse response =
        controller.request(fixture.notebook().getId(), "Asia/Shanghai");

    String markdown = response.getRequestMarkdown();
    assertThat(markdown, containsString("### Hola"));
    assertThat(markdown, containsString("1 previous session, last on 1989-01-06"));
    assertThat(markdown, containsString("### Gracias"));
    assertThat(markdown, containsString("- Tutoring status: not yet tutored"));
  }

  @Test
  void justReviewLogsOnSiblingUnderstandingTrackerDoNotChangeTutoringStatus()
      throws UnexpectedNoAccessRightException {
    Timestamp priorSessionAt = makeMe.aTimestamp().of(5, 10).fromShanghai().please();
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).fromShanghai().please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    MemoryTracker understandingTracker =
        makeMe.aMemoryTrackerFor(fixture.holaTracker().getNote()).please();
    makeMe
        .aRecallLogFor(understandingTracker)
        .grade(Grade.GOOD)
        .recordedAt(priorSessionAt)
        .please();

    LearningSessionRequestResponse response =
        controller.request(fixture.notebook().getId(), "Asia/Shanghai");

    assertThat(response.getRequestMarkdown(), not(containsString("previous session")));
    assertThat(response.getRequestMarkdown(), containsString("- Tutoring status: not yet tutored"));
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
