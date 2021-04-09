package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class ReviewPointTest {

    @Autowired
    MakeMe makeMe;
    @Autowired
    private Validator validator;
    User user;
    Note note;
    Link link;
    ReviewPoint reviewPoint = new ReviewPoint();

    @BeforeEach
    void setup() {
        user = makeMe.aUser().please();
        note = makeMe.aNote().byUser(user).please();
        Note note2 = makeMe.aNote().byUser(user).linkTo(note).please();
        link = note2.getLinks().get(0);
        reviewPoint.setUser(user);
    }

    @Test
    void validate() {
        reviewPoint.setNote(note);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(0, violations.size());
    }

}
