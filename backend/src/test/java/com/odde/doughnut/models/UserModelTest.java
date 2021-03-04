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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class UserModelTest {
    @Autowired MakeMe makeMe;

    UserModel userModel;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
    }

    @Nested
    class GetNewNotesToReview {

        @Test
        void whenThereIsNoNotesForUser() {
            UserModel anotherUser = makeMe.aUser().toModelPlease();
            makeMe.aNote().forUser(anotherUser).please();
            assertEquals(0, userModel.getNewNotesToReview().size());
        }

        @Nested
        class WhenThereAreTwoNotesForUser {
            NoteEntity note1;
            NoteEntity note2;

            @BeforeEach
            void setup() {
                note1 = makeMe.aNote().forUser(userModel).please();
                note2 = makeMe.aNote().forUser(userModel).please();
                makeMe.refresh(userModel);
            }

            @Test
            void shouldReturnTheNoteWhenThereAreTwo() {
                assertThat(userModel.getNewNotesToReview(), contains(note1, note2));
            }

            @Test
            void shouldReturnOneIfUsersDailySettignIsOne() {
                userModel.setAndSaveDailyNewNotesCount(1);
                assertThat(userModel.getNewNotesToReview(), hasSize(equalTo(1)));
            }

            @Test
            void shouldNotIncludeNotesThatAreAlreadyReviewed() {
                makeMe.aReviewPointFor(note1).by(userModel).please();
                assertThat(userModel.getNewNotesToReview(), hasSize(equalTo(1)));
                assertThat(userModel.getNewNotesToReview(), contains(note2));
            }

            @Test
            void shouldIncludeNotesThatAreReviewedByOtherPeople() {
                UserModel anotherUser = makeMe.aUser().toModelPlease();
                makeMe.aReviewPointFor(note1).by(anotherUser).please();
                assertThat(userModel.getNewNotesToReview(), hasSize(equalTo(2)));
            }

        }

    }

}