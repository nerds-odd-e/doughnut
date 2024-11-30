package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface MemoryTrackerRepository extends CrudRepository<MemoryTracker, Integer> {
  List<MemoryTracker> findAllByUserAndInitialReviewedAtGreaterThan(User user, Timestamp since);

  @Query(value = "SELECT count(*) " + byUserId, nativeQuery = true)
  int countByUserNotRemoved(Integer userId);

  @Query(
      value =
          "SELECT * "
              + byUserId
              + " AND rp.next_review_at <= :nextReviewAt ORDER BY rp.next_review_at",
      nativeQuery = true)
  Stream<MemoryTracker> findAllByUserAndNextReviewAtLessThanEqualOrderByNextReviewAt(
      Integer userId, @Param("nextReviewAt") Timestamp nextReviewAt);

  @Query(value = "SELECT * " + byUserId + "AND rp.note_id =:noteId", nativeQuery = true)
  MemoryTracker findByUserAndNote(Integer userId, @Param("noteId") Integer noteId);

  @Query(
      value =
          "SELECT * "
              + " FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + " ORDER BY initial_reviewed_at DESC LIMIT 100",
      nativeQuery = true)
  List<MemoryTracker> findLast100ByUser(Integer userId);

  @Query(
      value =
          "SELECT * "
              + " FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + " AND rp.last_reviewed_at IS NOT NULL "
              + " ORDER BY last_reviewed_at DESC LIMIT 100",
      nativeQuery = true)
  List<MemoryTracker> findLast100ReviewedByUser(Integer userId);

  String byUserId =
      " FROM memory_tracker rp "
          + " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_review IS FALSE ";
}
