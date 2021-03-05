package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class UserModelReviewPointsTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    UserModel anotherUser;
    Timestamp day1;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().withSpaceIntervals("1, 2, 4, 8").toModelPlease();
        anotherUser = makeMe.aUser().toModelPlease();
        day1 = makeMe.aTimestamp().of(1, 8).forWhereTheUserIs(userModel).please();
    }

    @Nested
    class WhenThereIsOneNote {
        NoteEntity noteEntity;

        @BeforeEach
        void setup() {
            noteEntity = makeMe.aNote().forUser(userModel).please();
        }

        @Test
        void whenThereIsNoReviewedNotesForUser() {
            makeMe.aReviewPointFor(noteEntity).by(anotherUser).please();
            assertThat(userModel.getMostUrgentReviewPointEntity(day1), is(nullValue()));
        }

        @Test
        void whenThereOneReviewedNotesForUser() {
            makeMe.aReviewPointFor(noteEntity).by(userModel).please();
            assertThat(userModel.getMostUrgentReviewPointEntity(day1).getNoteEntity(), equalTo(noteEntity));
        }

    }
}
