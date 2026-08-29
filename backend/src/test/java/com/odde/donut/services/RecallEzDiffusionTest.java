package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.answered;
import static com.odde.donut.services.RecallStatsTestFixtures.utc;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.QuestionType;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers plan {@code 008-probe-convergent-analyses} slice 3: trial selection/pooling for the
 * trailing three-local-morning MCQ EZ-diffusion diagnostic. The closed-form algebra itself is
 * covered by {@link EzDiffusionTest}.
 */
class RecallEzDiffusionTest {
  private static final ZoneId UTC = ZoneId.of("UTC");
  // RecallStatsTestFixtures.utc(day, hour): day 0 = 1989-01-01. Use day 20 as "today".
  private static final LocalDate TODAY_DATE = LocalDate.of(1989, 1, 21);

  private static List<RecallAnswerRow> mcqTrials(int count, int day, int correctCount) {
    List<RecallAnswerRow> rows = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Timestamp at = Timestamp.from(utc(day, 10).toInstant().plusSeconds(i));
      rows.add(answered(at, 2000 + i, i < correctCount, null, i, null, QuestionType.MCQ));
    }
    return rows;
  }

  @Test
  void noHistoryYieldsZeroTrialsZeroMorningsAndNullFit() {
    RecallEzDiffusion.Result result = RecallEzDiffusion.compute(List.of(), TODAY_DATE, UTC);

    assertThat(result.trialCount(), equalTo(0));
    assertThat(result.morningCount(), equalTo(0));
    assertThat(result.driftRate(), nullValue());
    assertThat(result.boundarySeparation(), nullValue());
    assertThat(result.nondecisionTimeMs(), nullValue());
  }

  @Test
  void nonMcqTrialsAreExcluded() {
    List<RecallAnswerRow> rows =
        List.of(answered(utc(20, 10), 2000, true, null, 1, null, QuestionType.SPELLING));

    RecallEzDiffusion.Result result = RecallEzDiffusion.compute(rows, TODAY_DATE, UTC);

    assertThat(result.trialCount(), equalTo(0));
    assertThat(result.morningCount(), equalTo(0));
  }

  @Test
  void trialsOutsideTheThreeMorningWindowAreExcluded() {
    // TODAY_DATE (1989-01-21) is utc()'s day 20; the window is [today-2, today] = days 18..20.
    // Day 17 is one day too early.
    List<RecallAnswerRow> rows = List.of(answered(utc(17, 10), 2000, true, null, 1));

    RecallEzDiffusion.Result result = RecallEzDiffusion.compute(rows, TODAY_DATE, UTC);

    assertThat(result.trialCount(), equalTo(0));
    assertThat(result.morningCount(), equalTo(0));
  }

  @Test
  void implausiblyFastAndHardDropSlowTrialsAreExcluded() {
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(20, 10), 100, true, null, 1), // below 300ms floor
            answered(utc(20, 11), 300_000, true, null, 2) // at hard-drop ceiling
            );

    RecallEzDiffusion.Result result = RecallEzDiffusion.compute(rows, TODAY_DATE, UTC);

    assertThat(result.trialCount(), equalTo(0));
  }

  @Test
  void belowThirtyTrialsYieldsNullFitButStillReportsCounts() {
    List<RecallAnswerRow> rows = mcqTrials(29, 20, 20);

    RecallEzDiffusion.Result result = RecallEzDiffusion.compute(rows, TODAY_DATE, UTC);

    assertThat(result.trialCount(), equalTo(29));
    assertThat(result.morningCount(), equalTo(1));
    assertThat(result.driftRate(), nullValue());
    assertThat(result.boundarySeparation(), nullValue());
    assertThat(result.nondecisionTimeMs(), nullValue());
  }

  @Test
  void thirtyTrialsOnASingleMorningStillFits() {
    // The window is rolling three mornings, but pooling still fits once trialCount clears 30,
    // even when every trial fell on one morning within the window.
    List<RecallAnswerRow> rows = mcqTrials(30, 20, 24);

    RecallEzDiffusion.Result result = RecallEzDiffusion.compute(rows, TODAY_DATE, UTC);

    assertThat(result.trialCount(), equalTo(30));
    assertThat(result.morningCount(), equalTo(1));
    assertThat(result.driftRate(), notNullValue());
    assertThat(result.boundarySeparation(), notNullValue());
    assertThat(result.nondecisionTimeMs(), notNullValue());
  }

  @Test
  void morningCountCountsDistinctQualifyingDatesAcrossTheWindow() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    rows.addAll(mcqTrials(10, 18, 8));
    rows.addAll(mcqTrials(10, 19, 8));
    rows.addAll(mcqTrials(10, 20, 8));

    RecallEzDiffusion.Result result = RecallEzDiffusion.compute(rows, TODAY_DATE, UTC);

    assertThat(result.trialCount(), equalTo(30));
    assertThat(result.morningCount(), equalTo(3));
    assertThat(result.driftRate(), notNullValue());
  }
}
