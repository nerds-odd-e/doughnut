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
          "SELECT * "
              + " FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + "   AND rp.assimilated_at > :since "
              + "   AND rp.removed_from_tracking IS FALSE",
      nativeQuery = true)
  List<MemoryTracker> findAllByUserAndAssimilatedAtGreaterThan(
      @Param("userId") Integer userId, @Param("since") Timestamp since);

  @Query(value = "SELECT count(*) " + byUserId, nativeQuery = true)
  int countByUserNotRemoved(Integer userId);

  @Query(
      value =
          "SELECT * "
              + byUserId
              + " AND rp.next_recall_at <= :nextRecallAt ORDER BY rp.next_recall_at",
      nativeQuery = true)
  Stream<MemoryTracker> findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
      Integer userId, @Param("nextRecallAt") Timestamp nextRecallAt);

  @Query(value = "SELECT * " + byUserId + "AND rp.note_id =:noteId", nativeQuery = true)
  List<MemoryTracker> findByUserAndNote(Integer userId, @Param("noteId") Integer noteId);

  @Query(
      value =
          "SELECT * "
              + " FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + " ORDER BY assimilated_at DESC LIMIT 100",
      nativeQuery = true)
  List<MemoryTracker> findLast100ByUser(Integer userId);

  @Query(
      value =
          "SELECT * "
              + " FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + " AND rp.last_recalled_at IS NOT NULL "
              + " ORDER BY last_recalled_at DESC LIMIT 100",
      nativeQuery = true)
  List<MemoryTracker> findLast100ReviewedByUser(Integer userId);

  String byUserId =
      " FROM memory_tracker rp "
          + " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_tracking IS FALSE ";
}
