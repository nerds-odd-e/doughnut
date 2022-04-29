package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Link;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface LinkRepository extends CrudRepository<Link, Integer> {
  @Query(value = "SELECT link.* FROM link where id in (:ids)", nativeQuery = true)
  Stream<Link> findAllByIds(String[] ids);
}
