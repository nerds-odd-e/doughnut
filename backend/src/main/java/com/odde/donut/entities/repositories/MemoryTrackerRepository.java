package com.odde.donut.entities.repositories;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerQueryFragments;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface MemoryTrackerRepository extends CrudRepository<MemoryTracker, Integer> {
  /**
   * True when {@code noteId} has a non-deleted, note-level (no property key), UNDERSTANDING-type
   * tracker for {@code userId} — the "target note is handled" condition for the property wiki-link
   * assimilation gate. Mirrors {@link MemoryTracker#isNoteLevelTracker()} and {@link
   * com.odde.donut.entities.MemoryTrackerType#UNDERSTANDING} via {@link
   * MemoryTrackerQueryFragments}.
   */
  @Query(
      "SELECT CASE WHEN COUNT(rp) > 0 THEN true ELSE false END FROM MemoryTracker rp"
          + " WHERE rp.note.id = :noteId"
          + " AND rp.user.id = :userId"
          + " AND rp.deletedAt IS NULL"
          + " AND "
          + MemoryTrackerQueryFragments.JPA_WHERE_NOTE_LEVEL_TRACKER
          + " AND "
          + MemoryTrackerQueryFragments.JPA_WHERE_UNDERSTANDING_TRACKER)
  boolean existsCompletedNoteLevelUnderstandingTracker(
      @Param("noteId") Integer noteId, @Param("userId") Integer userId);

  @Query(
      value =
          "SELECT rp.* FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + "   AND rp.assimilated_at > :since "
              + "   AND rp.removed_from_tracking IS FALSE "
              + "   AND rp.deleted_at IS NULL"
              + "   AND rp.type = 'UNDERSTANDING'",
      nativeQuery = true)
  List<MemoryTracker> findAllByUserAndAssimilatedAtGreaterThan(
      @Param("userId") Integer userId, @Param("since") Timestamp since);

  @Query(value = "SELECT count(*) " + byUserIdFrom, nativeQuery = true)
  int countByUserNotRemoved(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT rp.* "
              + byUserIdFrom
              + " AND rp.next_recall_at <= :nextRecallAt ORDER BY rp.next_recall_at, (rp.type = 'SPELLING') DESC",
      nativeQuery = true)
  Stream<MemoryTracker> findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
      @Param("userId") Integer userId, @Param("nextRecallAt") Timestamp nextRecallAt);

  @Query(
      value =
          "SELECT rp.* "
              + byUserIdCommissionedFrom
              + " AND rp.next_recall_at <= :nextRecallAt ORDER BY rp.next_recall_at",
      nativeQuery = true)
  Stream<MemoryTracker> findAllCommissionedByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
      @Param("userId") Integer userId, @Param("nextRecallAt") Timestamp nextRecallAt);

  @Query(
      value =
          "SELECT rp.* FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + "   AND rp.deleted_at IS NULL "
              + "   AND rp.note_id = :noteId",
      nativeQuery = true)
  List<MemoryTracker> findByUserAndNote(Integer userId, @Param("noteId") Integer noteId);

  @Query(
      value =
          "SELECT rp.* FROM memory_tracker rp "
              + byUserIdWhere
              + " ORDER BY rp.assimilated_at DESC LIMIT 100",
      nativeQuery = true)
  List<MemoryTracker> findLast100ByUser(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT rp.* FROM memory_tracker rp "
              + byUserIdWhere
              + " AND rp.last_recalled_at IS NOT NULL "
              + " ORDER BY rp.last_recalled_at DESC LIMIT 500",
      nativeQuery = true)
  List<MemoryTracker> findLast100RecalledByUser(@Param("userId") Integer userId);

  List<MemoryTracker> findByNote_IdIn(List<Integer> noteIds);

  String byUserIdFrom =
      " FROM memory_tracker rp "
          + " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_tracking IS FALSE "
          + "   AND rp.deleted_at IS NULL "
          + "   AND rp.type <> 'COMMISSIONED' ";

  String byUserIdCommissionedFrom =
      " FROM memory_tracker rp "
          + " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_tracking IS FALSE "
          + "   AND rp.deleted_at IS NULL "
          + "   AND rp.type = 'COMMISSIONED' ";

  String byUserIdWhere =
      " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_tracking IS FALSE "
          + "   AND rp.deleted_at IS NULL ";

  @Query(
      value =
          "SELECT MAX(rp.assimilated_at) FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + "   AND rp.deleted_at IS NULL",
      nativeQuery = true)
  Timestamp findLastAssimilationTimeByUser(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT MAX(rp.last_recalled_at) FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + "   AND rp.deleted_at IS NULL",
      nativeQuery = true)
  Timestamp findLastRecallTimeByUser(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT COUNT(*) FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + "   AND rp.deleted_at IS NULL",
      nativeQuery = true)
  long countByUser(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT mt.* FROM memory_tracker mt "
              + "WHERE mt.user_id = :userId "
              + "  AND mt.removed_from_tracking IS FALSE "
              + "  AND mt.deleted_at IS NULL "
              + "  AND mt.type <> 'SPELLING' "
              + "  AND mt.type <> 'COMMISSIONED' "
              + "  AND mt.next_recall_at <= :dueBy "
              + "  AND NOT EXISTS ("
              + "    SELECT 1 FROM recall_prompt rp "
              + "    LEFT JOIN mcq ON rp.mcq_id = mcq.id "
              + "    WHERE rp.memory_tracker_id = mt.id "
              + "      AND rp.answer_id IS NULL "
              + "      AND (mcq.id IS NULL OR mcq.is_contested = false)"
              + "  ) "
              + "  AND NOT EXISTS ("
              + "    SELECT 1 FROM question_generation_batch_request qgbr "
              + "    WHERE qgbr.memory_tracker_id = mt.id "
              + "      AND qgbr.status IN ('PENDING', 'OUTPUT_READY') "
              + "  ) "
              + "  AND ("
              + "    SELECT COUNT(*) FROM question_generation_batch_request qgbr "
              + "    WHERE qgbr.memory_tracker_id = mt.id "
              + "      AND qgbr.status = 'FAILED' "
              + "      AND NOT EXISTS ("
              + "        SELECT 1 FROM question_generation_batch_request imported "
              + "        WHERE imported.memory_tracker_id = mt.id "
              + "          AND imported.status = 'IMPORTED' "
              + "          AND imported.id > qgbr.id"
              + "      )"
              + "  ) < 2 "
              + "ORDER BY mt.next_recall_at",
      nativeQuery = true)
  List<MemoryTracker> findBatchQuestionGenerationCandidatesByUser(
      @Param("userId") Integer userId, @Param("dueBy") Timestamp dueBy);
}
