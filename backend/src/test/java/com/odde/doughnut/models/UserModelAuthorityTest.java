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
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class UserModelAuthorityTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    UserModel anotherUser;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        anotherUser = makeMe.aUser().toModelPlease();
    }

    @Nested
    class noteBelongsToACircle {
        CircleModel circleModel;
        Note note;

        @BeforeEach
        void setup() {
            circleModel = makeMe.aCircle().toModelPlease();
            note = makeMe.aNote().byUser(makeMe.aUser().please()).inCircle(circleModel).please();
        }

        @Test
        void userCanNotAccessNotesBelongToCircle() {
            assertFalse(userModel.getAuthorization().hasFullAuthority(note));
        }

        @Test
        void userCanAccessNotesBelongToCircleIfIsAMember() {
            makeMe.theCircle(circleModel).hasMember(userModel).please();
            assertTrue(userModel.getAuthorization().hasFullAuthority(note));
        }
    }


    @Nested
    class readAuthority {
        Note note;

        @BeforeEach
        void setup() {
            note = makeMe.aNote().please();
        }

        @Test
        void userCanNotAccessNotesBelongToCircle() {
            assertThrows(ResponseStatusException.class, ()->makeMe.aNullUserModel().getAuthorization().assertReadAuthorization(note));
        }

    }
}

