package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Thing;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ThingRepository extends CrudRepository<Thing, Integer> {
  @Query(value = "SELECT thing.* FROM thing where id in (:ids)", nativeQuery = true)
  Stream<Thing> findAllByIds(List<Integer> ids);
}
