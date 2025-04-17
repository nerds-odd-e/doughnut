package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface UserTokenRepository extends CrudRepository<UserToken, Integer> {
  List<UserToken> findAllByUser(User user);

  void deleteAllByUser(User user);
}
