package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    Timestamp baseDay;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().withSpaceIntervals("1, 2, 4, 8").toModelPlease();
        anotherUser = makeMe.aUser().toModelPlease();
        baseDay = makeMe.aTimestamp().of(1, 8).forWhereTheUserIs(userModel).please();
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
            assertThat(userModel.getMostUrgentReviewPointEntity(daysAfterBase(1)), is(nullValue()));
        }

        @ParameterizedTest
        @CsvSource({
                "0,   0, false",
                "0,   1, true",
                "0,   2, true",
                "0,  10, true",

                "1,   0, false",
                "1,   1, false",
                "1,   2, true",
                "1,  10, true",

                "2,   0, false",
                "2,   1, false",
                "2,   3, false",
                "2,   4, true",
                })
        void whenThereOneReviewedNotesForUser(Integer repetitionDone, Integer reviewDay, Boolean expectedToRepeat) {
            makeMe.aReviewPointFor(noteEntity).by(userModel).lastReviewedAt(baseDay).nthStrictRepetitionDone(repetitionDone).please();
            ReviewPointEntity mostUrgentReviewPointEntity = userModel.getMostUrgentReviewPointEntity(daysAfterBase(reviewDay));
            assertThat(mostUrgentReviewPointEntity != null, is(expectedToRepeat));
        }

    }

    private Timestamp daysAfterBase(Integer reviewDay) {
        return UserModel.addDaysToTimestamp(baseDay, reviewDay);
    }
}
