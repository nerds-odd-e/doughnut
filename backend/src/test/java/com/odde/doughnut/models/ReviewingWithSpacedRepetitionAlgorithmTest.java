package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.randomizers.NonRandomizer;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class ReviewingWithSpacedRepetitionAlgorithmTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    UserModel anotherUser;
    Timestamp baseDay;
    private Randomizer randomizer = new NonRandomizer();

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().withSpaceIntervals("1, 2, 4, 8").toModelPlease();
        anotherUser = makeMe.aUser().toModelPlease();
        baseDay = makeMe.aTimestamp().of(1, 8).forWhereTheUserIs(userModel).please();
    }

    @Nested
    class WhenThereIsOneNote {
        Note note;

        @BeforeEach
        void setup() {
            note = makeMe.aNote().byUser(userModel).please();
        }

        @Test
        void whenThereIsNoReviewedNotesForUser() {
            makeMe.aReviewPointFor(note).by(anotherUser).please();
            assertThat(getOneReviewPointNeedToRepeat(daysAfterBase(1)), is(nullValue()));
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
        void whenThereIsOneReviewedNotesForUser(Integer repetitionDone, Integer reviewDay, Boolean expectedToRepeat) {
            makeMe
                    .aReviewPointFor(note)
                    .by(userModel)
                    .nthStrictRepetitionOn(repetitionDone, baseDay)
                    .please();
            ReviewPoint mostUrgentReviewPoint = getOneReviewPointNeedToRepeat(daysAfterBase(reviewDay));
            assertThat(mostUrgentReviewPoint != null, is(expectedToRepeat));
        }

        @Nested
        class ReviewTimeIsAlignedByHalfADay {
            @ParameterizedTest
            @CsvSource({
                    "9,  6,    true",
                    "16, 0,    false",
                    "16, 15,   true",
                    "16, 17,   true",
            })
            void atHourInTheNextDay(Integer lastRepeatHour, Integer currentHour, Boolean expectedToRepeat) {
                baseDay = makeMe.aTimestamp().of(1, lastRepeatHour).forWhereTheUserIs(userModel).please();
                makeMe
                        .aReviewPointFor(note)
                        .by(userModel)
                        .nthStrictRepetitionOn(0, baseDay)
                        .please();
                final Timestamp timestamp = baseDay = makeMe.aTimestamp().of(2, currentHour).forWhereTheUserIs(userModel).please();
                ReviewPoint mostUrgentReviewPoint = getOneReviewPointNeedToRepeat(timestamp);
                assertThat(mostUrgentReviewPoint != null, is(expectedToRepeat));
            }
        }

    }

    private ReviewPoint getOneReviewPointNeedToRepeat(Timestamp timestamp) {
        Reviewing reviewing = userModel.createReviewing(timestamp);
        ReviewPointModel model = reviewing.getOneReviewPointNeedToRepeat(randomizer);
        if(model == null) return null;
        return model.getEntity();
    }

    private Timestamp daysAfterBase(Integer reviewDay) {
        return TimestampOperations.addDaysToTimestamp(baseDay, reviewDay);
    }
}
