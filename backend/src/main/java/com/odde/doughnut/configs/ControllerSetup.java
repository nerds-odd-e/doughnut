package com.odde.doughnut.configs;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;

@ControllerAdvice
public class ControllerSetup
{
    @Autowired
    private final GithubService githubService;
    @Autowired
    private ModelFactoryService modelFactoryService;
    @Autowired
    private CurrentUserFetcher currentUserFetcher;

    public ControllerSetup(GithubService githubService, ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
        this.githubService = githubService;
        this.modelFactoryService = modelFactoryService;
        this.currentUserFetcher = currentUserFetcher;
    }

    @InitBinder
    public void initBinder( WebDataBinder binder )
    {
        // trimming all strings coming from any user form
        StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(false);
        binder.registerCustomEditor(String.class, stringTrimmerEditor);
    }

    @SneakyThrows
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleSystemException(HttpServletRequest req, Exception e) {
        FailureReport failureReport = createFailureReport(e, req);
        Integer issueNumber = githubService.createGithubIssue(failureReport);
        failureReport.setIssueNumber(issueNumber);
        this.modelFactoryService.failureReportRepository.save(failureReport);

        throw e;
    }

    private FailureReport createFailureReport(Exception exception, HttpServletRequest req) {
        FailureReport failureReport = new FailureReport();
        failureReport.setErrorName(exception.getClass().getName());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        failureReport.setErrorDetail(getUserInfo() + getRequestInfo(req) + "# Stack trace\n"+sw.toString());
        this.modelFactoryService.failureReportRepository.save(failureReport);

        return failureReport;
    }

    private String getRequestInfo(HttpServletRequest req) {
        return "# request:\n"
                + "  Request URI:" + req.getRequestURI() + "\n"
                + "  Request Query:" + req.getQueryString() + "\n";
    }

    private String getUserInfo() {
        String result = "# user external Id: " + currentUserFetcher.getExternalIdentifier() + "\n";
        if (currentUserFetcher.getUser() != null) {
            result += "# user name: " + currentUserFetcher.getUser().getName() + "\n";
        }
        return result;
    }

}
