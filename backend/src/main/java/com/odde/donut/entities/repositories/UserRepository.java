package com.odde.donut.entities.repositories;

import com.odde.donut.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository
    extends CrudRepository<User, Integer>, PagingAndSortingRepository<User, Integer> {
  User findByExternalIdentifier(String externalIdentifier);

  Page<User> findAll(Pageable pageable);
}
