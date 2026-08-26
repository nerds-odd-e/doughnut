package com.odde.donut.services;

import com.odde.donut.entities.FailureReport;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface GithubService {
  String getIssueUrl(Integer issueNumber);

  Integer createGithubIssue(FailureReport failureReport) throws IOException, InterruptedException;

  List<Map<String, Object>> getOpenIssues() throws IOException, InterruptedException;

  void closeAllOpenIssues() throws IOException, InterruptedException;
}
