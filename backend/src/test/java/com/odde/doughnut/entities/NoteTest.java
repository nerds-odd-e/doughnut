package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.testability.MakeMe;
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
  User user;

  @Test
  void timeOrder() {
    Note parent = makeMe.aNote().please();
    Note note1 = makeMe.aNote().under(parent).please();
    Note note2 = makeMe.aNote().under(parent).please();
    makeMe.flush();
    makeMe.refresh(parent);
    assertThat(parent.getHierarchicalChildren(), containsInRelativeOrder(note1, note2));
  }

  @Nested
  class TopicConstructor {
    @Test
    void replaceParentPlaceholder() {
      Note parent = makeMe.aNote().titleConstructor("parent").inMemoryPlease();
      Note child = makeMe.aNote().under(parent).titleConstructor("%P is good").inMemoryPlease();
      assertThat(child.getTopic(), equalTo("[parent] is good"));
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
