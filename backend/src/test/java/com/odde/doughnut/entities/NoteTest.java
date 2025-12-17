package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteTest {

  @Autowired MakeMe makeMe;
  @Autowired NoteService noteService;

  @Test
  void timeOrder() {
    Note parent = makeMe.aNote().please();
    Note note1 = makeMe.aNote().under(parent).please();
    Note note2 = makeMe.aNote().under(parent).please();
    assertThat(parent.getChildren(), containsInRelativeOrder(note1, note2));
  }

  @Test
  void shortDetails() {
    Note note =
        makeMe
            .aNote()
            .details(
                "<strong>this is a very long sentence</strong> that contains very little meaning. The purpose is to test the truncate.")
            .please();
    assertThat(
        note.getNoteTopology().getShortDetails(),
        equalTo("this is a very long sentence that contains very li..."));
  }

  @Test
  void shortDetailsShouldBeNullIfEmpty() {
    Note note = makeMe.aNote().details("").please();
    assertThat(note.getNoteTopology().getShortDetails(), nullValue());
  }

  @Nested
  class TargetNote {
    Note parent;
    Note target;
    Note relationNote;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().title("parent").please();
      target = makeMe.aNote().please();
      relationNote = makeMe.aRelation().between(parent, target).please();
    }

    @Test
    void replaceParentPlaceholder() {
      assertThat(
          relationNote.getNoteTopology().getTargetNoteTopology().getTitle(),
          equalTo(target.getTitle()));
    }

    @Test
    void relationOfRelation() {
      Note relationOfRelation = makeMe.aRelation().between(parent, relationNote).please();
      assertThat(
          relationOfRelation
              .getNoteTopology()
              .getTargetNoteTopology()
              .getTargetNoteTopology()
              .getId(),
          equalTo(target.getId()));
    }
  }

  @Nested
  class Image {
    @Test
    void useParentImage() {
      Note parent = makeMe.aNote().imageUrl("https://img.com/xxx.jpg").inMemoryPlease();
      Note child = makeMe.aNote().under(parent).useParentImage().inMemoryPlease();
      assertThat(
          child.getImageWithMask().noteImage, equalTo(parent.getNoteAccessory().getImageUrl()));
    }

    @Test
    void UseParentImageWhenTheUrlIsEmptyString() {
      Note parent = makeMe.aNote().imageUrl("").inMemoryPlease();
      Note child = makeMe.aNote().under(parent).useParentImage().inMemoryPlease();
      assertNull(child.getImageWithMask());
    }
  }

  @Nested
  class NoteTypeField {
    Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().please();
    }

    @Test
    void shouldSetNoteTypeToConcept() {
      noteService.setNoteType(note, NoteType.CONCEPT);

      makeMe.refresh(note);
      assertThat(note.getNoteType(), equalTo(NoteType.CONCEPT));
    }

    @Test
    void shouldSetNoteTypeToExperience() {
      noteService.setNoteType(note, NoteType.EXPERIENCE);

      makeMe.refresh(note);
      assertThat(note.getNoteType(), equalTo(NoteType.EXPERIENCE));
    }

    @Test
    void shouldUpdateExistingNoteType() {
      noteService.setNoteType(note, NoteType.CONCEPT);
      makeMe.refresh(note);

      noteService.setNoteType(note, NoteType.SOURCE);

      makeMe.refresh(note);
      assertThat(note.getNoteType(), equalTo(NoteType.SOURCE));
    }

    @Test
    void shouldPersistNoteTypeChange() {
      noteService.setNoteType(note, NoteType.INITIATIVE);

      Note retrievedNote = noteService.findById(note.getId()).orElseThrow();
      assertThat(retrievedNote.getNoteType(), equalTo(NoteType.INITIATIVE));
    }
  }
}
