package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ReviewPointRepository extends CrudRepository<ReviewPoint, Integer> {
  List<ReviewPoint> findAllByUserAndInitialReviewedAtGreaterThan(User user, Timestamp since);

  @Query(value = "SELECT count(*) " + byUserId, nativeQuery = true)
  int countByUserNotRemoved(Integer userId);

  @Query(
      value =
          "SELECT * "
              + byUserId
              + " AND rp.next_review_at <= :nextReviewAt ORDER BY rp.next_review_at",
      nativeQuery = true)
  Stream<ReviewPoint> findAllByUserAndNextReviewAtLessThanEqualOrderByNextReviewAt(
      Integer userId, @Param("nextReviewAt") Timestamp nextReviewAt);

  @Query(value = "SELECT * " + byUserId + "AND rp.note_id =:noteId", nativeQuery = true)
  ReviewPoint findByUserAndNote(Integer userId, @Param("noteId") Integer noteId);

  @Query(
      value = "SELECT * " + byUserId + " ORDER BY initial_reviewed_at DESC LIMIT 100",
      nativeQuery = true)
  List<ReviewPoint> findLast100ByUser(Integer userId);

  String byUserId =
      " FROM review_point rp "
          + " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_review IS FALSE ";
}
