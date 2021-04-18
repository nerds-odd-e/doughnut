package com.odde.doughnut.configs;

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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

@ControllerAdvice
public class ControllerSetup
{
    @Autowired
    private final GithubService githubService;
    @Autowired
    public ModelFactoryService modelFactoryService;

    public ControllerSetup(GithubService githubService, ModelFactoryService modelFactoryService) {
        this.githubService = githubService;
        this.modelFactoryService = modelFactoryService;
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
    public String handleSystemException(Exception e) {
        FailureReport failureReport = createFailureReport(e);
        Integer issueNumber = githubService.createGithubIssue(failureReport);
        failureReport.setIssueNumber(issueNumber);
        this.modelFactoryService.failureReportRepository.save(failureReport);

        throw e;
    }

    private FailureReport createFailureReport(Exception exception) {
        FailureReport failureReport = new FailureReport();
        failureReport.setErrorName(exception.getClass().getName());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        failureReport.setErrorDetail(sw.toString());
        this.modelFactoryService.failureReportRepository.save(failureReport);

        return failureReport;
    }

}
