package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteAsTreeNodeTest {
  Note topLevel;
  @Autowired MakeMe makeMe;

  @BeforeEach
  void setup() {
    topLevel = makeMe.aNote("topLevel").please();
  }

  @Test
  void topLevelNoteHasNoParent() {
    assertThat(topLevel.getParent(), nullValue());
  }

  @Test
  void childNoteLinksToImmediateParent() {
    Note subject = makeMe.aNote("subject").under(topLevel).please();
    Note sibling = makeMe.aNote("sibling").under(topLevel).please();

    assertThat(subject.getParent(), is(topLevel));
    assertThat(sibling.getParent(), is(topLevel));
  }
}
