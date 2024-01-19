package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReviewingInitialReviewTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  UserModel anotherUser;
  Timestamp day1;
  Timestamp day0;
  Reviewing reviewingOnDay1;

  public ReviewPoint getOneInitialReviewPoint(Reviewing reviewing) {
    return reviewing.getDueInitialReviewPoints().findFirst().orElse(null);
  }

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    anotherUser = makeMe.aUser().toModelPlease();
    day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    day0 = makeMe.aTimestamp().of(0, 8).fromShanghai().please();
    reviewingOnDay1 = userModel.createReviewing(day1, ZoneId.of("Asia/Shanghai"));
  }

  @Test
  void whenThereIsNoNotesForUser() {
    makeMe.aNote().creatorAndOwner(anotherUser).please();
    assertThat(getOneInitialReviewPoint(reviewingOnDay1), is(nullValue()));
    assertThat(reviewingOnDay1.getReviewStatus().toInitialReviewCount, equalTo(0));
  }

  @Nested
  class WhenThereAreTwoNotesForUser {
    Note note1;
    Note note2;

    @BeforeEach
    void setup() {
      note1 = makeMe.aNote("note1").creatorAndOwner(userModel).please();
      note2 = makeMe.aNote("note2").creatorAndOwner(userModel).please();
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
      Link note1ToNote2;
      Note anotherNote;

      @BeforeEach
      void thereIsALinkAndAnotherNote() {
        note1ToNote2 = makeMe.aLink().between(note1, note2).please();
        anotherNote = makeMe.aNote("another note").creatorAndOwner(userModel).please();
        makeMe.refresh(userModel.getEntity());
      }

      private List<ReviewPoint> getAllDueReviewPoints() {
        return reviewingOnDay1.getDueInitialReviewPoints().collect(Collectors.toList());
      }

      @Test
      void shouldReturnLinkBeforeAnotherNote() {
        List<ReviewPoint> reviewPoints = getAllDueReviewPoints();
        assertThat(reviewPoints, hasSize(4));
        assertThat(reviewPoints.get(0).getNote(), equalTo(note1));
        assertThat(reviewPoints.get(1).getNote(), equalTo(note2));
        assertThat(reviewPoints.get(2).getLink(), equalTo(note1ToNote2));
        assertThat(reviewPoints.get(3).getNote(), equalTo(anotherNote));
      }

      @Nested
      class WithLevels {
        @BeforeEach
        void Note1And2HaveDifferentLevels() {
          makeMe.aReviewSettingFor(note1).level(5).please();
          makeMe.aReviewSettingFor(note2).level(2).please();
        }

        @Test
        void shouldReturnReviewPointForLowerLevelNoteOrLink() {
          List<ReviewPoint> reviewPoints = getAllDueReviewPoints();
          assertThat(reviewPoints, hasSize(4));
          assertThat(reviewPoints.get(0).getNote(), equalTo(anotherNote));
          assertThat(reviewPoints.get(1).getNote(), equalTo(note2));
          assertThat(reviewPoints.get(2).getNote(), equalTo(note1));
          assertThat(reviewPoints.get(3).getLink(), equalTo(note1ToNote2));
        }

        @Test
        void shouldReturnLinksOrderedByLevels() {
          Link aLevel2Link = makeMe.aLink().between(anotherNote, note2).please();
          makeMe.refresh(userModel.getEntity());
          List<ReviewPoint> reviewPoints = getAllDueReviewPoints();
          assertThat(reviewPoints, hasSize(5));
          assertThat(reviewPoints.get(0).getNote(), equalTo(anotherNote));
          assertThat(reviewPoints.get(1).getNote(), equalTo(note2));
          assertThat(reviewPoints.get(2).getLink(), equalTo(aLevel2Link));
          assertThat(reviewPoints.get(4).getLink(), equalTo(note1ToNote2));
        }

        @Test
        void shouldNotReturnReviewPointForLinkIfCreatedByOtherPeople() {
          makeMe.theNote(note1).notebookOwnership(makeMe.aUser().please()).please();
          makeMe.refresh(userModel.getEntity());
          List<ReviewPoint> reviewPoints = getAllDueReviewPoints();
          assertThat(reviewPoints, hasSize(2));
          assertThat(reviewPoints.get(0).getNote(), equalTo(anotherNote));
          assertThat(reviewPoints.get(1).getNote(), equalTo(note2));
        }
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
      void shouldNotCountSkippedReviewPoint() {
        makeMe
            .aReviewPointFor(note1)
            .by(userModel)
            .initiallyReviewedOn(day1)
            .removedFromReview()
            .please();
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), is(note2));
      }

      @Test
      void shouldIncludeNotesThatAreReviewedByOtherPeople() {
        makeMe.aReviewPointFor(note1).by(anotherUser).initiallyReviewedOn(day1).please();
        assertThat(getOneInitialReviewPoint(reviewingOnDay1).getNote(), equalTo(note1));
      }

      @Test
      void theDailyCountShouldNotBeResetOnSameDayDifferentHour() {
        makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
        Timestamp day1_23 = makeMe.aTimestamp().of(1, 23).fromShanghai().please();
        Reviewing reviewing = userModel.createReviewing(day1_23, ZoneId.of("Asia/Shanghai"));
        assertThat(getOneInitialReviewPoint(reviewing), is(nullValue()));
      }

      @Test
      void theDailyCountShouldBeResetOnNextDay() {
        makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
        Timestamp day2 = makeMe.aTimestamp().of(2, 1).fromShanghai().please();
        Reviewing reviewing = userModel.createReviewing(day2, ZoneId.of("Asia/Shanghai"));
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
      Note top = makeMe.aNote().creatorAndOwner(anotherUser).please();
      note1 = makeMe.aNote().under(top).please();
      note2 = makeMe.aNote().under(top).please();
      makeMe
          .aSubscription()
          .forNotebook(top.getNotebook())
          .forUser(userModel.entity)
          .daily(1)
          .please();
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

    @Test
    void reviewedMoreThanPlanned() {
      makeMe.aReviewPointFor(note1).by(userModel).initiallyReviewedOn(day1).please();
      makeMe.aReviewPointFor(note2).by(userModel).initiallyReviewedOn(day1).please();
      assertThat(getOneInitialReviewPoint(reviewingOnDay1), nullValue());
    }
  }

  @Nested
  class NotesInCircle {
    Note top;

    @BeforeEach
    void setup() {
      Circle please = makeMe.aCircle().hasMember(userModel).please();
      top = makeMe.aNote().inCircle(please).please();
      makeMe.refresh(userModel.getEntity());
    }

    @Test
    void shouldNotBeReviewed() {
      assertThat(getOneInitialReviewPoint(reviewingOnDay1), is(nullValue()));
    }
  }
}
