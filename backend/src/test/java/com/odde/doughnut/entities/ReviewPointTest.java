package com.odde.doughnut.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class ReviewPointTest {

  @Autowired MakeMe makeMe;
  @Autowired private Validator validator;
  User user;
  Note note;
  Link link;
  ReviewPoint reviewPoint;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    note = makeMe.aNote().creatorAndOwner(user).please();
    Note note2 = makeMe.aNote().creatorAndOwner(user).linkTo(note).please();
    link = note2.getLinks().get(0);
    reviewPoint = makeMe.aReviewPointFor(note).by(user).inMemoryPlease();
  }

  @Test
  void validate() {
    reviewPoint.setThing(note.getThing());
    Set<ConstraintViolation<User>> violations = validator.validate(user);
    assertEquals(0, violations.size());
  }
}
