package com.odde.donut.services;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.repositories.RecallLogRepository;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time backfill of {@code stability_before}, {@code difficulty_before}, and {@code
 * retrievability} on {@code recall_log} rows that predate the live instrumentation added in {@code
 * MemoryTrackerService#persistRecallLog}. Those columns are NULL on any row created before that
 * change shipped.
 *
 * <p>For each tracker with NULL rows, replays its recall logs oldest-first through the same
 * production {@link MemoryTracker} methods the live path uses ({@code applyGrade}, {@code
 * adjustForConfusion}, {@code retrievabilityAt}) on a scratch, never-persisted tracker — no FSRS
 * math is reimplemented here, and a "before" snapshot is always taken before the row's own
 * grade/confusion effect is applied, exactly as the live call sites do.
 *
 * <p>A row's stored {@code elapsed_hours} is a checksum on the replay: if replaying from the
 * tracker's reconstructed state produces a different elapsed-hours value than what was actually
 * recorded, the replay state can no longer be trusted. That row, and every later row for the same
 * tracker, is left {@code NULL} rather than guessed. Reaching a row that already has a {@code
 * stability_before} means the live-instrumented boundary has been reached — everything from there
 * on for that tracker was already recorded live, so replay stops.
 *
 * <p>Deliberately not a Flyway migration (see {@code db-migration.mdc}): one-off data repair
 * belongs outside the permanent migration chain, and this needs the FSRS entity code to reproduce
 * the exact before/after values, not a pure-SQL migration. Not wired to run automatically — invoke
 * {@link #run} manually against the target database.
 */
public final class RecallLogMemoryStateBackfill {

  public record Result(int trackersWithGaps, int rowsBackfilled) {}

  private RecallLogMemoryStateBackfill() {}

  public static Result run(RecallLogRepository recallLogRepository) {
    List<Integer> trackerIds =
        recallLogRepository.findDistinctMemoryTrackerIdsWithNullStabilityBefore();
    int rowsBackfilled = 0;
    for (Integer trackerId : trackerIds) {
      rowsBackfilled += backfillTracker(recallLogRepository, trackerId);
    }
    return new Result(trackerIds.size(), rowsBackfilled);
  }

  private static int backfillTracker(RecallLogRepository recallLogRepository, Integer trackerId) {
    List<RecallLog> orderedLogs =
        recallLogRepository.findAllByMemoryTracker_IdOrderByRecordedAtAscIdAsc(trackerId);
    MemoryTracker replay = MemoryTracker.buildMemoryTrackerForNote(null);
    // Every real tracker has a non-null assimilatedAt/nextRecallAt from creation
    // (MemoryTrackerAssimilation#initializeNewTracker); adjustForConfusion()/
    // calculateNextRecallAt() fall back to assimilatedAt when lastRecalledAt is still null (a
    // New tracker's first-ever row can be a CONFUSION row). The scheduling result is never read
    // back here — only stability/difficulty matter — so any non-null placeholder is safe.
    replay.setAssimilatedAt(new Timestamp(0));
    replay.setNextRecallAt(replay.calculateNextRecallAt());
    List<RecallLog> toSave = new ArrayList<>();

    for (RecallLog log : orderedLogs) {
      if (log.getStabilityBefore() != null) {
        break;
      }
      if (replay.elapsedHoursUntil(log.getRecordedAt()) != log.getElapsedHours()) {
        break;
      }

      log.setStabilityBefore(replay.getStability());
      log.setDifficultyBefore(replay.getDifficulty());
      log.setRetrievability(replay.retrievabilityAt(log.getRecordedAt()));
      toSave.add(log);

      if (log.isConfusion()) {
        replay.adjustForConfusion(log.getRecordedAt());
      } else {
        replay.applyGrade(log.getRecordedAt(), log.getGrade());
      }
    }

    recallLogRepository.saveAll(toSave);
    return toSave.size();
  }
}
