package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.AttributedNoteContent;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AttributedNoteContentControllerTests extends NotebookGitWebContentControllerTestBase {

  @Autowired AttributedNoteContentController controller;

  @Test
  void readsTheNoteWithItsSourceAttribution() throws Exception {
    Timestamp updatedAt = Timestamp.from(Instant.parse("2026-09-06T04:05:06Z"));
    Notebook notebook = createGitBackedNotebook("Physics");
    Folder outer = makeMe.aFolder().notebook(notebook).name("Mechanics").please();
    Folder inner = makeMe.aFolder().parentFolder(outer).name("Energy").please();
    Note note =
        makeMe
            .aNote()
            .folder(inner)
            .title("Kinetic")
            .content(ACCEPTED_CONTENT)
            .updatedAt(updatedAt)
            .please();
    String head = snapshotCurrentPortableTree(notebook).getAcceptedGitObjectId();

    AttributedNoteContent read = controller.getAttributedContent(note);

    assertThat(
        read,
        equalTo(
            new AttributedNoteContent(
                note.getId(),
                "Kinetic",
                "Physics",
                "Mechanics / Energy",
                ACCEPTED_CONTENT,
                updatedAt,
                head)));
    assertThat(head, matchesPattern("[0-9a-f]{40}"));
  }

  @Test
  void aContentEditInTheNotebookChangesTheRevisionOfALaterRead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("Read").content(ACCEPTED_CONTENT).please();
    Note edited =
        makeMe.aNote().notebook(notebook).title("Edited").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    String earlier = controller.getAttributedContent(note).acceptedRevision();

    textContentController.updateNoteContent(edited, contentDto(EDITED_CONTENT));

    assertThat(controller.getAttributedContent(note).acceptedRevision(), not(earlier));
    assertThat(earlier, matchesPattern("[0-9a-f]{40}"));
  }

  @Test
  void refusesANoteTheUserCannotRead() {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getAttributedContent(note));
  }

  @Test
  void aNotebookWithoutGitBindingReadsWithoutRevision() throws Exception {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

    assertThat(controller.getAttributedContent(note).acceptedRevision(), nullValue());
  }
}
