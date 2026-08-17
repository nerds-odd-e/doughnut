package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.RecallLog;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface RecallLogRepository extends CrudRepository<RecallLog, Integer> {
  List<RecallLog> findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(Integer memoryTrackerId);
}
