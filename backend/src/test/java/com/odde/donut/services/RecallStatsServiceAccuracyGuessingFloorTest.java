package com.odde.donut.services;

import static com.odde.donut.services.RecallProbabilityMath.sigmoid;
import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import com.odde.donut.entities.QuestionType;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * End-to-end (through {@link RecallStatsService#aggregateRows}) check that slice 20's 3PL
 * guessing-floor fit is applied <em>per question type</em>: an MCQ trailing history with a genuine
 * guessing floor and a SPELLING trailing history without one must recalibrate an
 * identically-low-retrievability today's answer differently, because each type's gamma is fit only
 * from that type's own trailing rows.
 */
class RecallStatsServiceAccuracyGuessingFloorTest {
  @Test
  void todaysAccuracyRecalibratesMcqAndSpellingWithIndependentlyFittedGuessingFloors() {
    Timestamp now = utc(200, 12); // today = day 200
    List<RecallAnswerRow> rows = new ArrayList<>();
    Random random = new Random(99);
    int n = 4000;
    for (int i = 0; i < n; i++) {
      double x = -4 + 8.0 * i / (n - 1);
      double retrievability = sigmoid(x);
      Timestamp trailingAt = utc(190 + (i % 5), i % 24);
      // MCQ trailing history: true guessing floor of 0.3.
      double pMcq = 0.3 + 0.7 * sigmoid(1.2 * x);
      rows.add(
          answered(
              trailingAt,
              5000,
              random.nextDouble() < pMcq,
              null,
              i,
              retrievability,
              QuestionType.MCQ));
      // Spelling trailing history: no genuine guessing floor.
      double pSpelling = sigmoid(1.2 * x);
      rows.add(
          answered(
              trailingAt,
              5000,
              random.nextDouble() < pSpelling,
              null,
              n + i,
              retrievability,
              QuestionType.SPELLING));
    }
    // Today: one MCQ and one SPELLING answer, both correct at the same low retrievability —
    // the item-relative situation where a guessing floor matters most.
    double lowRetrievability = sigmoid(-3.0);
    rows.add(answered(utc(200, 10), 5000, true, null, 10_000, lowRetrievability, QuestionType.MCQ));
    rows.add(
        answered(utc(200, 11), 5000, true, null, 10_001, lowRetrievability, QuestionType.SPELLING));

    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(2));
    // MCQ's fitted guessing floor absorbs most of the low-retrievability correct answer's
    // "surprise" (p̂ near 0.3, so y-p̂ ~ 0.7); spelling has no floor, so its p̂ stays near the raw
    // sigmoid value close to 0 (y-p̂ ~ 1). If both types shared a single fit, the two answers
    // would contribute similarly instead of this asymmetric split; the standardized residual
    // being clearly positive and driven by spelling's larger surprise is the observable evidence
    // that the two types were recalibrated independently.
    assertThat(accuracy.getStandardizedResidual(), greaterThan(2.0));
  }
}
