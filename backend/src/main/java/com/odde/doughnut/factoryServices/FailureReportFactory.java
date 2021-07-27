package com.odde.doughnut.factoryServices;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.services.GithubService;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class FailureReportFactory {
    private final HttpServletRequest req;
    private final Exception exception;
    private final CurrentUserFetcher currentUserFetcher;
    private final GithubService githubService;
    private final ModelFactoryService modelFactoryService;

    public FailureReportFactory(HttpServletRequest req, Exception exception, CurrentUserFetcher currentUserFetcher, GithubService githubService, ModelFactoryService modelFactoryService) {
        this.req = req;
        this.exception = exception;
        this.currentUserFetcher = currentUserFetcher;
        this.githubService = githubService;
        this.modelFactoryService = modelFactoryService;
    }

    public void createUnlessAllowed() throws IOException, InterruptedException {
        if(exception instanceof ResponseStatusException) return;

        FailureReport failureReport = createFailureReport();
        Integer issueNumber = githubService.createGithubIssue(failureReport);
        failureReport.setIssueNumber(issueNumber);
        this.modelFactoryService.failureReportRepository.save(failureReport);
    }

    private FailureReport createFailureReport() {
        FailureReport failureReport = new FailureReport();
        failureReport.setErrorName(exception.getClass().getName());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        failureReport.setErrorDetail(getUserInfo() + getRequestInfo() + "# Stack trace\n"+sw.toString());
        this.modelFactoryService.failureReportRepository.save(failureReport);

        return failureReport;
    }

    private String getRequestInfo() {
        return "# request:\n"
                + "  Request URI:" + req.getRequestURI() + "\n"
                + "  Request Query:" + req.getQueryString() + "\n";
    }

    private String getUserInfo() {
        String result = "# user external Id: " + currentUserFetcher.getExternalIdentifier() + "\n";
        if (currentUserFetcher.getUser() != null && currentUserFetcher.getUser().loggedIn()) {
            result += "# user name: " + currentUserFetcher.getUser().getName() + "\n";
        }
        return result;
    }

}
