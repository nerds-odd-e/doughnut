package com.odde.doughnut.services;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.entities.repositories.FailureReportRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FailureReportService {
  private final FailureReportRepository failureReportRepository;

  public FailureReportService(FailureReportRepository failureReportRepository) {
    this.failureReportRepository = failureReportRepository;
  }

  public Iterable<FailureReport> getAllFailureReports() {
    return failureReportRepository.findAll();
  }

  public void deleteFailureReports(List<Integer> ids) {
    ids.forEach(
        id -> {
          failureReportRepository.findById(id).ifPresent(failureReportRepository::delete);
        });
  }
}
