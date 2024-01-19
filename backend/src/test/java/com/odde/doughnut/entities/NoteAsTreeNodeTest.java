package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.testability.MakeMe;
import java.util.List;
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
public class NoteAsTreeNodeTest {
  Note topLevel;
  @Autowired MakeMe makeMe;

  @BeforeEach
  void setup() {
    topLevel = makeMe.aNote("topLevel").please();
  }

  @Nested
  class GetAncestors {

    @Test
    void topLevelNoteHaveEmptyAncestors() {
      List<Note> ancestors = topLevel.getAncestors();
      assertThat(ancestors, empty());
    }

    @Test
    void childHasParentInAncestors() {
      Note subject = makeMe.aNote("subject").under(topLevel).please();
      Note sibling = makeMe.aNote("sibling").under(topLevel).please();

      List<Note> ancestry = subject.getAncestors();
      assertThat(ancestry, contains(topLevel));
      assertThat(ancestry, not(contains(sibling)));
    }
  }
}
