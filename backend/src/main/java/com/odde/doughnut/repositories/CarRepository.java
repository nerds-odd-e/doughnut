package com.odde.doughnut.repositories;

import com.odde.doughnut.entities.Car;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
  Optional<Car> findByPlayerId(String playerId);
}
