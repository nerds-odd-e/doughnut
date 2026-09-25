package com.odde.donut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.testability.SpringTestBase;
import org.junit.jupiter.api.Test;

public class CircleTest extends SpringTestBase {
  @Test
  void invitationCode() {
    Circle cirle1 = makeMe.aCircle().inMemoryPlease();
    Circle cirle2 = makeMe.aCircle().inMemoryPlease();
    assertThat(cirle1.getInvitationCode(), is(not(equalTo(cirle2.getInvitationCode()))));
  }
}
