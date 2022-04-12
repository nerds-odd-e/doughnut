package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ReviewPointRepository extends CrudRepository<ReviewPoint, Integer> {
  List<ReviewPoint> findAllByUserAndInitialReviewedAtGreaterThan(User user, Timestamp since);

  @Query(value = "SELECT count(*) " + byUser, nativeQuery = true)
  int countByUserNotRemoved(@Param("user") User user);

  @Query(
      value =
          "SELECT * "
              + byUser
              + " AND rp.next_review_at <= :nextReviewAt ORDER BY rp.next_review_at",
      nativeQuery = true)
  List<ReviewPoint> findAllByUserAndNextReviewAtLessThanEqualOrderByNextReviewAt(
      @Param("user") User user, @Param("nextReviewAt") Timestamp nextReviewAt);

  @Query(value = "SELECT * " + byUser + "AND rp.note_id =:#{#note.id}", nativeQuery = true)
  ReviewPoint findByUserAndNote(User user, Note note);

  @Query(value = "SELECT * " + byUser + "AND rp.link_id =:#{#link.id}", nativeQuery = true)
  ReviewPoint findByUserAndLink(User user, Link link);

  String byUser =
      " FROM review_point rp "
          + " WHERE rp.user_id = :user "
          + "   AND rp.removed_from_review IS FALSE ";
}
