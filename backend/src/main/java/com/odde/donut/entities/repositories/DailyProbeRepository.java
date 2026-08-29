package com.odde.donut.entities.repositories;

import com.odde.donut.entities.DailyProbe;
import com.odde.donut.entities.User;
import java.sql.Timestamp;
import org.springframework.data.repository.CrudRepository;

public interface DailyProbeRepository extends CrudRepository<DailyProbe, Integer> {
  boolean existsByUserAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
      User user, Timestamp startInclusive, Timestamp endExclusive);
}
