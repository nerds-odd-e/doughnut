package com.odde.donut.entities.repositories;

import com.odde.donut.entities.FailureReport;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface FailureReportRepository extends CrudRepository<FailureReport, Integer> {
  Optional<FailureReport> findTopByOrderByIdDesc();
}
