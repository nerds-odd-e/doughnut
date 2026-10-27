package com.odde.donut.entities.repositories;

import com.odde.donut.entities.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository
    extends CrudRepository<User, Integer>, PagingAndSortingRepository<User, Integer> {
  User findByExternalIdentifier(String externalIdentifier);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT user FROM User user WHERE user.externalIdentifier = :externalIdentifier")
  User findByExternalIdentifierForUpdate(String externalIdentifier);

  Page<User> findAll(Pageable pageable);
}
