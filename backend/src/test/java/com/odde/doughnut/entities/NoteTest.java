package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

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
    Note linkingNote;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().titleConstructor("parent").please();
      target = makeMe.aNote().please();
      linkingNote = makeMe.aReification().between(parent, target).please();
    }

    @Test
    void replaceParentPlaceholder() {
      assertThat(
          linkingNote.getNoteTopology().getObjectNoteTopology().getTitleOrPredicate(),
          equalTo(target.getTitleConstructor()));
    }

    @Test
    void linkOfLink() {
      Note linkOfLink = makeMe.aReification().between(parent, linkingNote).please();
      assertThat(
          linkOfLink.getNoteTopology().getObjectNoteTopology().getObjectNoteTopology().getId(),
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
}
