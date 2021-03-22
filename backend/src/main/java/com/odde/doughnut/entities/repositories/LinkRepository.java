package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LinkRepository extends CrudRepository<LinkEntity, Integer> {
    @Query( value = "SELECT link.* from link " + byUserWhereThereIsNoReviewPoint, nativeQuery = true)
    List<LinkEntity> findByUserWhereThereIsNoReviewPoint(@Param("userId") Integer userId);

    @Query( value = "SELECT count(1) as count from link " + byUserWhereThereIsNoReviewPoint, nativeQuery = true)
    int countByUserWhereThereIsNoReviewPoint(@Param("userId") Integer userId);

    String byUserWhereThereIsNoReviewPoint = " LEFT JOIN ("
            + "     SELECT id, link_id FROM review_point WHERE user_id = :userId"
            + " ) as rp"
            + " ON link.id = rp.link_id "
            + " WHERE "
            + "   rp.id IS NULL ";

}
