package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Player;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface PlayerRepository extends CrudRepository<Player, Integer> {
  List<Player> findByIdOfGame(Integer gameId);
}
