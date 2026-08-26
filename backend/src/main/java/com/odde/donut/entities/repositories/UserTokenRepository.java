package com.odde.donut.entities.repositories;

import com.odde.donut.entities.UserToken;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface UserTokenRepository extends CrudRepository<UserToken, Integer> {
  public UserToken findByToken(String token);

  List<UserToken> findByUserId(Integer userId);
}
