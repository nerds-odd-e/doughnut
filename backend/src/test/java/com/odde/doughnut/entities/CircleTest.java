package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CircleTest {

  @Autowired MakeMe makeMe;

  @Test
  void invitationCode() {
    Circle cirle1 = makeMe.aCircle().inMemoryPlease();
    Circle cirle2 = makeMe.aCircle().inMemoryPlease();
    assertThat(cirle1.getInvitationCode(), is(not(equalTo(cirle2.getInvitationCode()))));
  }
}
