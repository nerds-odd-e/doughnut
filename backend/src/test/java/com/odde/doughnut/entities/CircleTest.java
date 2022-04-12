package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
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
