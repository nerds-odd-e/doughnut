package com.odde.doughnut.entities;

import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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

