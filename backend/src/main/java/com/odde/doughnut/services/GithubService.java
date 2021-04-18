package com.odde.doughnut.services;

import com.odde.doughnut.entities.FailureReport;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface GithubService {
    Integer createGithubIssue(FailureReport failureReport) throws IOException, InterruptedException;

    List<Map<String, Object>> getOpenIssues() throws IOException, InterruptedException;

    void closeAllOpenIssues() throws IOException, InterruptedException;
}
