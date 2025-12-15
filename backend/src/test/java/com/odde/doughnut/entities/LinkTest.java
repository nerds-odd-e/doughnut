package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
public class LinkTest {

  @Autowired MakeMe makeMe;

  @Nested
  class LevelOfLink {
    Note subject;
    Note target;

    @BeforeEach
    void setup() {
      subject = makeMe.aNote().please();
      target = makeMe.aNote().please();
    }

    @Test
    void shouldGetSourceLevelWhenItIsHigher() {
      makeMe.theNote(subject).level(5).please();
      Note link = makeMe.aRelation().between(subject, target).inMemoryPlease();
      assertThat(link.getRecallSetting().getLevel(), is(5));
    }

    @Test
    void shouldGetTargetLevelWhenItIsHigher() {
      makeMe.theNote(target).level(5).please();
      Note link = makeMe.aRelation().between(subject, target).inMemoryPlease();
      assertThat(link.getRecallSetting().getLevel(), is(5));
    }
  }
}
