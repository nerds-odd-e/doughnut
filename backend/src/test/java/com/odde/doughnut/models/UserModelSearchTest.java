package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class UserModelSearchTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    UserModel anotherUser;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        anotherUser = makeMe.aUser().toModelPlease();
    }

    @Test
    void userCanNotAccessNotesBelongToCircle() {
    }
}

