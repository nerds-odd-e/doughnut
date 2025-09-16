package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Step;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface StepRepository extends CrudRepository<Step, Integer> {
  List<Step> findByPlayerId(Integer playerId);
}
