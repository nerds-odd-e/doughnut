package com.odde.donut.services;

import com.odde.donut.controllers.dto.FailureReportDeletionResultDTO;
import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.repositories.FailureReportRepository;
import java.io.IOException;
import java.util.ArrayList;
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

  public FailureReportDeletionResultDTO deleteFailureReports(List<Integer> ids) {
    List<String> unresolvedGithubIssueUrls = new ArrayList<>();
    for (Integer id : ids) {
      Optional<FailureReport> failureReport = failureReportRepository.findById(id);
      if (failureReport.isEmpty()) {
        continue;
      }
      FailureReport report = failureReport.get();
      Integer issueNumber = report.getIssueNumber();
      if (issueNumber != null) {
        try {
          githubService.closeIssueAsCompleted(issueNumber);
        } catch (IOException | InterruptedException e) {
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
          unresolvedGithubIssueUrls.add(githubService.getIssueUrl(issueNumber));
        }
      }
      failureReportRepository.delete(report);
    }
    return new FailureReportDeletionResultDTO(unresolvedGithubIssueUrls);
  }
}
