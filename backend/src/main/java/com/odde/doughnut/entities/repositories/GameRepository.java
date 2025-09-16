package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Game;
import org.springframework.data.repository.CrudRepository;

public interface GameRepository extends CrudRepository<Game, Integer> {
}
