package com.odde.doughnut.repositories;

import com.odde.doughnut.entities.Round;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {
  @Query("SELECT r FROM Round r WHERE r.playerId = :playerId ORDER BY r.createdAt DESC LIMIT 1")
  Optional<Round> findByPlayerId(@Param("playerId") String playerId);

  @Modifying
  @Query("DELETE FROM Round r WHERE r.playerId = :playerId")
  void deleteByPlayerId(@Param("playerId") String playerId);
}
