package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.UserEntity;
import org.springframework.data.repository.CrudRepository;

import java.sql.Timestamp;
import java.util.List;

public interface ReviewPointRepository extends CrudRepository<ReviewPointEntity, Integer> {
    List<ReviewPointEntity> findAllByUserEntityAndLastReviewedAtGreaterThan(UserEntity userEntity, Timestamp since);
}
