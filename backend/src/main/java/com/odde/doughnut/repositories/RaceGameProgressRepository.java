package com.odde.doughnut.repositories;

import com.odde.doughnut.entities.RaceGameProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceGameProgressRepository extends JpaRepository<RaceGameProgress, Long> {
  Optional<RaceGameProgress> findByPlayerId(String playerId);
}
