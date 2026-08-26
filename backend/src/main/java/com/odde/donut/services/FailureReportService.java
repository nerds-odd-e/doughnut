package com.odde.donut.services;

import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.repositories.FailureReportRepository;
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
