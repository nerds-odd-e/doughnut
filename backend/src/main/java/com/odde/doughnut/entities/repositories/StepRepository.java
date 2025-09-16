package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Step;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface StepRepository extends CrudRepository<Step, Integer> {
  List<Step> findByPlayerId(Integer playerId);
}
