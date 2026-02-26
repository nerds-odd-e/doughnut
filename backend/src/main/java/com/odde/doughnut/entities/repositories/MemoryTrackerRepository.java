package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.MemoryTracker;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface MemoryTrackerRepository extends CrudRepository<MemoryTracker, Integer> {
  @Query(
      value =
          "SELECT rp.* FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + "   AND rp.assimilated_at > :since "
              + "   AND rp.removed_from_tracking IS FALSE "
              + "   AND rp.deleted_at IS NULL",
      nativeQuery = true)
  List<MemoryTracker> findAllByUserAndAssimilatedAtGreaterThan(
      @Param("userId") Integer userId, @Param("since") Timestamp since);

  @Query(value = "SELECT count(*) " + byUserIdFrom, nativeQuery = true)
  int countByUserNotRemoved(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT rp.* "
              + byUserIdFrom
              + " AND rp.next_recall_at <= :nextRecallAt ORDER BY rp.next_recall_at",
      nativeQuery = true)
  Stream<MemoryTracker> findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
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
          + "   AND rp.deleted_at IS NULL ";

  String byUserIdWhere =
      " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_tracking IS FALSE "
          + "   AND rp.deleted_at IS NULL ";
}
