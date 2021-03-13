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

@SpringBootTest
@Transactional
public class UserEntityTest {

    @Autowired
    MakeMe makeMe;
    @Autowired
    private Validator validator;
    UserEntity userEntity;

    @BeforeEach
    void setup() {
        userEntity = makeMe.aUser().please();
    }

    @Test
    void validate() {
        Set<ConstraintViolation<UserEntity>> violations = validator.validate(userEntity);
        assertEquals(0, violations.size());
    }

    @Test
    void validateName() {
        userEntity.setName("");
        Set<ConstraintViolation<UserEntity>> violations = validator.validate(userEntity);
        assertEquals(1, violations.size());
    }

    @Test
    void validateSpacing() {
        userEntity.setSpaceIntervals("1,2a");
        Set<ConstraintViolation<UserEntity>> violations = validator.validate(userEntity);
        assertEquals(1, violations.size());
    }

    @Test
    void validateSpacingValid() {
        userEntity.setSpaceIntervals("1,2,33, 444");
        Set<ConstraintViolation<UserEntity>> violations = validator.validate(userEntity);
        assertEquals(0, violations.size());
    }

}

