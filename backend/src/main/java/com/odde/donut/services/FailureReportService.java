package com.odde.donut.services;

import com.odde.donut.controllers.dto.FailureReportDeletionResultDTO;
import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.repositories.FailureReportRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FailureReportService {
  private final FailureReportRepository failureReportRepository;
  private final GithubService githubService;

  public FailureReportService(
      FailureReportRepository failureReportRepository, GithubService githubService) {
    this.failureReportRepository = failureReportRepository;
    this.githubService = githubService;
  }

  public Iterable<FailureReport> getAllFailureReports() {
    return failureReportRepository.findAll();
  }

  public FailureReportDeletionResultDTO deleteFailureReports(List<Integer> ids)
      throws IOException, InterruptedException {
    for (Integer id : ids) {
      Optional<FailureReport> failureReport = failureReportRepository.findById(id);
      if (failureReport.isEmpty()) {
        continue;
      }
      FailureReport report = failureReport.get();
      if (report.getIssueNumber() != null) {
        githubService.closeIssueAsCompleted(report.getIssueNumber());
      }
      failureReportRepository.delete(report);
    }
    return new FailureReportDeletionResultDTO(List.of());
  }
}
