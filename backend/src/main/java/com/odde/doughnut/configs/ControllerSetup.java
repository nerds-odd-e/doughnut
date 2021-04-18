package com.odde.doughnut.configs;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.services.FailureReportFactory;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.TestabilitySettings;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ControllerSetup
{
    @Value("${spring.github-for-issues.repo}")
    private String githubForIssuesRepo;
    @Autowired
    private final GithubService githubService;
    @Autowired
    private ModelFactoryService modelFactoryService;
    @Autowired
    private CurrentUserFetcher currentUserFetcher;
    @Autowired
    private TestabilitySettings testabilitySettings;

    public ControllerSetup(GithubService githubService, ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher, TestabilitySettings testabilitySettings) {
        this.githubService = githubService;
        this.modelFactoryService = modelFactoryService;
        this.currentUserFetcher = currentUserFetcher;
        this.testabilitySettings = testabilitySettings;
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
        FailureReportFactory failureReportFactory = new FailureReportFactory(req, e, currentUserFetcher, githubService, modelFactoryService);
        failureReportFactory.create();

        throw e;
    }


}
