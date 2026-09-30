package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.NoteReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NoteReifyPropertyRefusalTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired NoteRepository noteRepository;
  @Autowired NoteReferenceService noteReferenceService;

  Note source;
  Note relationshipNote;
  String originalContent;
  String originalRelationshipContent;
  long originalNoteCount;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    source =
        makeMe
            .aNote("Src")
            .notebookOwnedBy(currentUser.getUser())
            .content(
                "---\n"
                    + "plain: training\n"
                    + "related:\n"
                    + "  - \"[[Other]]\"\n"
                    + "around: see [[Other]]\n"
                    + "nowhere: \"[[Nowhere]]\"\n"
                    + "---\nbody")
            .please();
    makeMe.aNote("Other").underSameNotebookAs(source).please();
    relationshipNote =
        makeMe
            .aNote("Src relates to Other")
            .underSameNotebookAs(source)
            .content(
                "---\n"
                    + "type: Relationship\n"
                    + "relation: relates to\n"
                    + "source: \"[[Src]]\"\n"
                    + "target: \"[[Other]]\"\n"
                    + "---\n")
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(source);
    originalContent = source.getContent();
    originalRelationshipContent = relationshipNote.getContent();
    originalNoteCount = noteRepository.count();
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "missing | The note has no property missing.",
        "plain   | Only a property whose value is a link to a note can be reified.",
        "related | Only a property whose value is a link to a note can be reified.",
        "around  | Only a property whose value is a link to a note can be reified.",
        "nowhere | The link [[Nowhere]] does not name an existing note.",
      })
  void isRefusedWithTheReasonAndChangesNothing(String key, String message) {
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.reifyProperty(source, key));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(exception.getReason(), equalTo(message));
    makeMe.refresh(source);
    assertThat(source.getContent(), equalTo(originalContent));
    assertThat(noteRepository.count(), equalTo(originalNoteCount));
  }

  @ParameterizedTest
  @CsvSource({"source", "target"})
  void structuralKeyIsRefusedWithTheReasonAndChangesNothing(String key) {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.reifyProperty(relationshipNote, key));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(exception.getReason(), equalTo("A structural property cannot be reified."));
    makeMe.refresh(relationshipNote);
    assertThat(relationshipNote.getContent(), equalTo(originalRelationshipContent));
    assertThat(noteRepository.count(), equalTo(originalNoteCount));
  }
}
