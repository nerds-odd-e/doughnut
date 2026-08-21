package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.LearningSessionRequestResponse;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.focusContext.FocusContextConstants;
import java.sql.Timestamp;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LearningSessionRequestRelatedNotesTests extends LearningSessionControllerTestBase {

  @Test
  void relatedNoteLinkedFromBothSessionItemsAppearsOnceInRelatedNotes()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook =
        makeMe
            .aNotebook()
            .creatorAndOwner(currentUser.getUser())
            .name("Spanish conversation")
            .please();
    makeMe.aNote().notebook(notebook).title("Saludos").content("Greetings").please();
    commissionSpanishSessionItems(
        notebook, dayTwo, "Hello. See [[Saludos]]", "Thank you. See [[Saludos]]");

    LearningSessionRequestResponse response = controller.request(notebook.getId(), "Asia/Shanghai");
    String markdown = response.getRequestMarkdown();

    assertThat(markdown, containsString(FocusContextConstants.RELATED_NOTES_OPEN_MARKER));
    assertThat(markdown, containsString("Greetings"));
    assertThat(
        markdown,
        containsString(FocusContextConstants.RELATED_NOTES_CLOSE_TAG + "\n<how_to_report>"));
    int relatedNotesStart = markdown.indexOf(FocusContextConstants.RELATED_NOTES_OPEN_MARKER);
    int relatedNotesEnd =
        markdown.indexOf(FocusContextConstants.RELATED_NOTES_CLOSE_TAG, relatedNotesStart)
            + FocusContextConstants.RELATED_NOTES_CLOSE_TAG.length();
    String relatedNotes = markdown.substring(relatedNotesStart, relatedNotesEnd);
    assertThat(relatedNotes.split(Pattern.quote("Title: Saludos"), -1).length - 1, equalTo(1));
  }

  @Test
  void sessionItemLinkedFromAnotherSessionItemIsNotInRelatedNotes()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook =
        makeMe
            .aNotebook()
            .creatorAndOwner(currentUser.getUser())
            .name("Spanish conversation")
            .please();
    commissionSpanishSessionItems(notebook, dayTwo, "Hello. See [[Gracias]]", "Thank you");

    LearningSessionRequestResponse response = controller.request(notebook.getId(), "Asia/Shanghai");
    String markdown = response.getRequestMarkdown();

    assertThat(
        markdown,
        not(containsString(FocusContextConstants.RETRIEVED_NOTE_OPEN_MARKER + "\nTitle: Hola")));
    assertThat(
        markdown,
        not(containsString(FocusContextConstants.RETRIEVED_NOTE_OPEN_MARKER + "\nTitle: Gracias")));
  }

  private void commissionSpanishSessionItems(
      Notebook notebook, Timestamp dueAt, String holaContent, String graciasContent) {
    Note hola = makeMe.aNote().notebook(notebook).title("Hola").content(holaContent).please();
    Note gracias =
        makeMe.aNote().notebook(notebook).title("Gracias").content(graciasContent).please();
    makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dueAt).please();
    makeMe.aMemoryTrackerFor(gracias).commissioned().nextRecallAt(dueAt).please();
  }
}
