package com.odde.doughnut.testability;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.services.GithubService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class NullGithubService implements GithubService {
    public NullGithubService(String githubForIssuesRepo) {
    }

    @Override
    public Integer createGithubIssue(FailureReport failureReport) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public List<Map<String, Object>> getOpenIssues() throws IOException, InterruptedException {
        return null;
    }

    @Override
    public void closeAllOpenIssues() throws IOException, InterruptedException {

    }
}
