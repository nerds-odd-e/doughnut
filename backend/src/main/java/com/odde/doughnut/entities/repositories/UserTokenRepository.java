package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.UserToken;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface UserTokenRepository extends CrudRepository<UserToken, Integer> {
  public UserToken findByToken(String token);

  List<UserToken> findByUserId(Integer userId);
}
