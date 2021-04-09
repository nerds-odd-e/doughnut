package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface ReviewPointRepository extends CrudRepository<ReviewPointEntity, Integer> {
    List<ReviewPointEntity> findAllByUserEntityAndInitialReviewedAtGreaterThan(UserEntity userEntity, Timestamp since);

    @Query( value = "SELECT count(*) " + byUserEntity, nativeQuery = true)
    int countByUserEntityNotRemoved(@Param("userEntity") UserEntity userEntity);

    @Query( value = "SELECT * " + byUserEntity + " AND rp.next_review_at <= :nextReviewAt ORDER BY rp.next_review_at", nativeQuery = true)
    List<ReviewPointEntity> findAllByUserEntityAndNextReviewAtLessThanEqualOrderByNextReviewAt(@Param("userEntity") UserEntity userEntity, @Param("nextReviewAt") Timestamp nextReviewAt);

    void deleteAllByNote(Note note);

    String byUserEntity = " FROM review_point rp "
            + " WHERE rp.user_id = :userEntity "
            + "   AND rp.removed_from_review IS FALSE ";

}
