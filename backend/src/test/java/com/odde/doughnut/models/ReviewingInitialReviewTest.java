package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class ReviewingInitialReviewTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  UserModel anotherUser;
  Timestamp day1;
  Timestamp day0;
  Reviewing reviewingOnDay1;

  public ReviewPoint getOneInitialReviewPoint(Reviewing reviewing) {
    return reviewing.getDueInitialReviewPoint().findFirst().orElse(null);
  }

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    anotherUser = makeMe.aUser().toModelPlease();
    day1 = makeMe.aTimestamp().of(1, 8).forWhereTheUserIs(userModel).please();
    day0 = makeMe.aTimestamp().of(0, 8).forWhereTheUserIs(userModel).please();
    reviewingOnDay1 = userModel.createReviewing(day1);
  }

  @Test
  void whenThereIsNoNotesForUser() {
    makeMe.aNote().byUser(anotherUser).please();
    assertThat(getOneInitialReviewPoint(reviewingOnDay1), is(nullValue()));
    assertThat(reviewingOnDay1.getReviewStatus().toInitialReviewCount, equalTo(0));
  }

  @Nested
  class WhenThereAreTwoNotesForUser {
    Note note1;
    Note note2;

    @BeforeEach
    void setup() {
      note1 = makeMe.aNote().byUser(userModel).please();
      note2 = makeMe.aNote().byUser(userModel).please();
      makeMe.refresh(userModel.getEntity());
    }

    @Test
    void shouldReturnTheFirstNoteAndThenTheSecondWhenThereAreTwo() {
      assertThat(reviewingOnDay1.getReviewStatus().toInitialReviewCount, equalTo(2));
      assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note1));
      makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
      assertThat(reviewingOnDay1.getReviewStatus().toInitialReviewCount, equalTo(1));
      assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note2));
    }

    @Test
    void shouldReturnTheSecondNoteIfItsLevelIsLower() {
      makeMe.aReviewSettingFor(note1).level(2).please();
      makeMe.aReviewSettingFor(note2).level(1).please();
      assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note2));
    }

    @Test
    void shouldNotIncludeNoteThatIsSkippedForReview() {
      makeMe.theNote(note1).skipReview().linkTo(note2).please();
      assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note2));
    }

    @Nested
    class ReviewPointFromLink {
      @BeforeEach
      void Note1And2SkippedReview_AndThereIsALink() {
        makeMe.theNote(note2).skipReview().please();
        makeMe.theNote(note1).skipReview().linkTo(note2).please();
      }

      @Test
      void shouldReturnReviewPointForLink() {
        assertThat(
            getOneInitialReviewPoint(reviewingOnDay1).getLink().getSourceNote(), equalTo(note1));
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), is(nullValue()));
      }

      @Test
      void shouldNotReturnReviewPointForLinkWhenTheNoteIsNotSkipped() {
        makeMe
            .theNote(note2)
            .cancelSkipReview()
            .createdAt(new Timestamp(System.currentTimeMillis() + 10000))
            .please();
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note2));
      }

      @Test
      void shouldNotReturnReviewPointForLinkWhenTheSourceNoteIsNotSkipped() {
        makeMe
            .theNote(note1)
            .cancelSkipReview()
            .createdAt(new Timestamp(System.currentTimeMillis() + 10000))
            .please();
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note1));
      }

      @Test
      void shouldNotReturnReviewPointForLinkWhenTheNoteIsNotReviewed() {
        makeMe.theNote(note2).cancelSkipReview().please();
        makeMe.aReviewPointFor(note2).by(userModel).initiallyReviewedOn(day0).please();
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getLink(), is(notNullValue()));
      }

      @Test
      void shouldReturnReviewPointForLinkIfCreatedEarlierThanNote() {
        Note note3 =
            makeMe
                .aNote()
                .byUser(userModel)
                .createdAt(new Timestamp(System.currentTimeMillis() + 10000))
                .please();
        assertThat(
            getOneInitialReviewPoint(reviewingOnDay1).getLink().getSourceNote(), equalTo(note1));
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), is(nullValue()));
      }

      @Test
      void shouldGetNoteInCreatedOrder() {
        Note note3 =
            makeMe
                .aNote()
                .byUser(userModel)
                .createdAt(new Timestamp(System.currentTimeMillis() + 10000))
                .please();
        Note note4 =
            makeMe
                .aNote()
                .byUser(userModel)
                .createdAt(new Timestamp(System.currentTimeMillis() - 10000))
                .please();
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note4));
      }

      @Test
      void shouldReturnReviewPointForLinkInCreatedOrder() {
        makeMe
            .aLink()
            .between(note2, note1)
            .createdAt(new Timestamp(System.currentTimeMillis() - 10000))
            .please();
        assertThat(
            getOneInitialReviewPoint(reviewingOnDay1).getLink().getSourceNote(), equalTo(note2));
      }

      @Test
      void shouldNotReturnReviewPointForLinkIfCreatedByOtherPeople() {
        makeMe.theNote(note1).notebookOwnership(makeMe.aUser().please()).please();
        assertThat(getOneInitialReviewPoint(reviewingOnDay1), is(nullValue()));
      }
    }

    @Nested
    class WhenTheUserSetToReview1NewNoteOnlyPerDay {

      @BeforeEach
      void setup() {
        userModel.setAndSaveDailyNewNotesCount(1);
      }

      @Test
      void shouldReturnOneIfUsersDailySettingIsOne() {
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note1));
      }

      @Test
      void shouldNotIncludeNotesThatAreAlreadyReviewed() {
        makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
        assertThat(getOneInitialReviewPoint(reviewingOnDay1), is(nullValue()));
      }

      @Test
      void shouldIncludeNotesThatAreReviewedByOtherPeople() {
        makeMe.aReviewPointFor(note1).by(anotherUser).initiallyReviewedOn(day1).please();
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note1));
      }

      @Test
      void theDailyCountShouldNotBeResetOnSameDayDifferentHour() {
        makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
        Timestamp day1_23 = makeMe.aTimestamp().of(1, 23).forWhereTheUserIs(userModel).please();
        Reviewing reviewing = userModel.createReviewing(day1_23);
        assertThat(getOneInitialReviewPoint(reviewing), is(nullValue()));
      }

      @Test
      void theDailyCountShouldBeResetOnNextDay() {
        makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
        Timestamp day2 = makeMe.aTimestamp().of(2, 1).forWhereTheUserIs(userModel).please();
        Reviewing reviewing = userModel.createReviewing(day2);
        assertThat(getOneInitialReviewPoint(reviewing).getNote(), equalTo(note2));
      }
    }
  }

  @Nested
  class ReviewSubscribedNote {
    Note note1;
    Note note2;

    @BeforeEach
    void setup() {
      User anotherUser = makeMe.aUser().please();
      Note top = makeMe.aNote().byUser(anotherUser).please();
      note1 = makeMe.aNote().under(top).please();
      note2 = makeMe.aNote().under(top).please();
      makeMe.aSubscription().forNotebook(top.getNotebook()).forUser(userModel.entity).please();
      makeMe.refresh(userModel.getEntity());
    }

    @Test
    void shouldReturnReviewPointForNote() {
      assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note1));
    }

    @Test
    void shouldReturnReviewPointForLink() {
      makeMe.theNote(note2).skipReview().please();
      makeMe.theNote(note1).skipReview().linkTo(note2).please();
      assertThat(
          getOneInitialReviewPoint(reviewingOnDay1).getLink().getSourceNote(), equalTo(note1));
    }
  }

  @Nested
  class NotesInCircle {
    Note top;

    @BeforeEach
    void setup() {
      Circle please = makeMe.aCircle().hasMember(userModel).please();
      top = makeMe.aNote().byUser(userModel).inCircle(please).please();
      makeMe.refresh(userModel.getEntity());
    }

    @Test
    void shouldNotBeReviewed() {
      assertThat(getOneInitialReviewPoint(reviewingOnDay1), is(nullValue()));
    }
  }
}
