package com.odde.doughnut.repositories;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Integer> {
    User findByExternalIdentifier(String externalIdentifier);
}
