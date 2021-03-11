package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
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
public class User_NewNotesToReviewTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    UserModel anotherUser;
    Timestamp day1;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        anotherUser = makeMe.aUser().toModelPlease();
        day1 = makeMe.aTimestamp().of(1, 8).forWhereTheUserIs(userModel).please();
    }

    private ReviewPointEntity getOneInitialReviewPointEntity(Timestamp timestamp) {
        Reviewing reviewing = new Reviewing(userModel, timestamp);
        return reviewing.getOneInitialReviewPointEntity();
    }

    @Test
    void whenThereIsNoNotesForUser() {
        makeMe.aNote().byUser(anotherUser).please();
        assertThat(getOneInitialReviewPointEntity(day1), is(nullValue()));
    }

    @Nested
    class WhenThereAreTwoNotesForUser {
        NoteEntity note1;
        NoteEntity note2;

        @BeforeEach
        void setup() {
            note1 = makeMe.aNote().byUser(userModel).please();
            note2 = makeMe.aNote().byUser(userModel).please();
            makeMe.refresh(userModel);
        }

        @Test
        void shouldReturnTheFirstNoteAndThenTheSecondWhenThereAreTwo() {
            assertThat(getOneInitialReviewPointEntity(day1).getNoteEntity(), equalTo(note1));
            makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
            assertThat(getOneInitialReviewPointEntity(day1).getNoteEntity(), equalTo(note2));
        }

        @Test
        void shouldNotIncludeNoteThatIsSkippedForReview() {
            makeMe.theNote(note1).skipReview().please();
            assertThat(getOneInitialReviewPointEntity(day1).getNoteEntity(), equalTo(note2));
        }

        @Nested
        class WhenTheUserSetToReview1NewNoteOnlyPerDay {

            @BeforeEach
            void setup() {
                userModel.setAndSaveDailyNewNotesCount(1);
            }

            @Test
            void shouldReturnOneIfUsersDailySettignIsOne() {
                assertThat(getOneInitialReviewPointEntity(day1).getNoteEntity(), equalTo(note1));
            }


            @Test
            void shouldNotIncludeNotesThatAreAlreadyReviewed() {
                makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
                assertThat(getOneInitialReviewPointEntity(day1), is(nullValue()));
            }

            @Test
            void shouldIncludeNotesThatAreReviewedByOtherPeople() {
                makeMe.aReviewPointFor(note1).by(anotherUser).initiallyReviewedOn(day1).please();
                assertThat(getOneInitialReviewPointEntity(day1).getNoteEntity(), equalTo(note1));
            }

            @Test
            void theDailyCountShouldNotBeResetOnSameDayDifferentHour() {
                makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
                Timestamp day1_23 = makeMe.aTimestamp().of(1, 23).forWhereTheUserIs(userModel).please();
                assertThat(getOneInitialReviewPointEntity(day1_23), is(nullValue()));
            }

            @Test
            void theDailyCountShouldBeResetOnNextDay() {
                makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
                Timestamp day2 = makeMe.aTimestamp().of(2, 1).forWhereTheUserIs(userModel).please();
                assertThat(getOneInitialReviewPointEntity(day2).getNoteEntity(), equalTo(note2));
            }

        }

    }

}
