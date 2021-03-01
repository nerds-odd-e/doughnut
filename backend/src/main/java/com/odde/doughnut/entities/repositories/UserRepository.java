package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.UserEntity;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserEntity, Integer> {
    UserEntity findByExternalIdentifier(String externalIdentifier);
}
