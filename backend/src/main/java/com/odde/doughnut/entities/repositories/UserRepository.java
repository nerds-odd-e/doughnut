package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Integer> {
  User findByExternalIdentifier(String externalIdentifier);
}
