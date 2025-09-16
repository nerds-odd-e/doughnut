package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Player;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface PlayerRepository extends CrudRepository<Player, Integer> {
  List<Player> findByIdOfGame(Integer gameId);
}
