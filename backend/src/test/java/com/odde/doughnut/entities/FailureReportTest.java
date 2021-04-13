package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@Transactional
public class FailureReportTest {

    @Autowired
    MakeMe makeMe;
    @Autowired
    private Validator validator;

    @Nested
    class ValidationTest {
        private Validator validator;
        private final FailureReport failureReport = makeMe.aFailureReport().inMemoryPlease();

        @BeforeEach
        public void setUp() {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @Test
        public void errorNameEmpty() {
            failureReport.setErrorName("");
            assertThat(getViolations(), is(not(empty())));
        }

        @Test
        public void errorDetailEmpty() {
            failureReport.setErrorDetail("");
            assertThat(getViolations(), is(not(empty())));
        }

        @Test
        public void errorCreatedDatetimeEmpty() {
            failureReport.setCreateDatetime(null);
            assertThat(getViolations(), is(not(empty())));
        }

        private Set<ConstraintViolation<FailureReport>> getViolations() {
            return validator.validate(failureReport);
        }

    }

}
