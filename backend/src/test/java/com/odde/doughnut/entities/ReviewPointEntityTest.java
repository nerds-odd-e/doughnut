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
public class ReviewPointEntityTest {

    @Autowired
    MakeMe makeMe;
    @Autowired
    private Validator validator;
    UserEntity userEntity;
    Note note;
    Link link;
    ReviewPointEntity reviewPointEntity = new ReviewPointEntity();

    @BeforeEach
    void setup() {
        userEntity = makeMe.aUser().please();
        note = makeMe.aNote().byUser(userEntity).please();
        Note note2 = makeMe.aNote().byUser(userEntity).linkTo(note).please();
        link = note2.getLinks().get(0);
        reviewPointEntity.setUserEntity(userEntity);
    }

    @Test
    void validate() {
        reviewPointEntity.setNote(note);
        Set<ConstraintViolation<UserEntity>> violations = validator.validate(userEntity);
        assertEquals(0, violations.size());
    }

}
