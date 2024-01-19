package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReviewingWithSpacedRepetitionAlgorithmTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  UserModel anotherUser;
  private Randomizer randomizer = new NonRandomizer();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().withSpaceIntervals("1, 2, 4, 8").toModelPlease();
    anotherUser = makeMe.aUser().toModelPlease();
  }

  @Nested
  class WhenThereIsOneNote {
    Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().creatorAndOwner(userModel).please();
    }

    @Test
    void whenThereIsNoReviewedNotesForUser() {
      ReviewPoint reviewPoint = makeMe.aReviewPointFor(note).by(anotherUser).please();
      assertThat(getOneReviewPointNeedToRepeat(daysAfterBase(reviewPoint, 1)), is(nullValue()));
    }

    @ParameterizedTest
    @CsvSource({
      "1,   0, false",
      "1,   1, true",
      "1,   2, true",
      "1,  10, true",
      "2,   0, false",
      "2,   1, false",
      "2,   2, true",
      "2,  10, true",
      "3,   0, false",
      "3,   1, false",
      "3,   3, false",
      "3,   4, true",
    })
    void whenThereIsOneReviewedNotesForUser(
        Integer repetitionDone, Integer reviewDay, Boolean expectedToRepeat) {
      ReviewPoint reviewPoint =
          makeMe
              .aReviewPointFor(note)
              .by(userModel)
              .afterNthStrictRepetition(repetitionDone)
              .please();
      ReviewPoint mostUrgentReviewPoint =
          getOneReviewPointNeedToRepeat(daysAfterBase(reviewPoint, reviewDay));
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
      void atHourInTheNextDay(
          Integer lastRepeatHour, Integer currentHour, Boolean expectedToRepeat) {
        ReviewPoint reviewPoint = makeMe.aReviewPointFor(note).by(userModel).please();
        reviewPoint.setNextReviewAt(
            makeMe.aTimestamp().of(2, lastRepeatHour).fromShanghai().please());
        final Timestamp timestamp = makeMe.aTimestamp().of(2, currentHour).fromShanghai().please();
        ReviewPoint mostUrgentReviewPoint = getOneReviewPointNeedToRepeat(timestamp);
        assertThat(mostUrgentReviewPoint != null, is(expectedToRepeat));
      }
    }

    @Nested
    class EarlyAndLateReview {
      @ParameterizedTest
      @CsvSource({
        "0, 0,  100",
        "0, 1,  110",
        "2, -1,  115",
        "2, 0, 120",
        "2, 1, 115",
        "2, 100, 100",
      })
      void aReviewPointHasBeenReviewedStrictly(
          int ntimes, Integer daysDelay, int expectedForgettingCurveIndex) {
        ReviewPointModel reviewPoint =
            makeMe
                .aReviewPointFor(note)
                .by(userModel)
                .afterNthStrictRepetition(ntimes)
                .toModelPlease();
        reviewPoint.markAsRepeated(
            TimestampOperations.addHoursToTimestamp(
                reviewPoint.getEntity().getNextReviewAt(), daysDelay * 24),
            true);
        assertThat(
            reviewPoint.getEntity().getForgettingCurveIndex(),
            equalTo(expectedForgettingCurveIndex));
      }
    }
  }

  private ReviewPoint getOneReviewPointNeedToRepeat(Timestamp timestamp) {
    return userModel
        .getReviewPointsNeedToRepeat(timestamp, ZoneId.of("Asia/Shanghai"))
        .findFirst()
        .orElse(null);
  }

  private Timestamp daysAfterBase(ReviewPoint reviewPoint, Integer reviewDay) {
    return TimestampOperations.addHoursToTimestamp(reviewPoint.getLastReviewedAt(), reviewDay * 24);
  }
}
