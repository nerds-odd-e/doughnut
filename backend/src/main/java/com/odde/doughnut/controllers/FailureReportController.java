package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/failure-report-list")
public class FailureReportController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;

    public FailureReportController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("")
    public String failureReport(Model model) throws NoAccessRightException {
        return "vuejsed";
    }

    @GetMapping("/show/{failureReport}")
    public String show(@PathVariable(name = "failureReport") FailureReport failureReport, Model model) throws NoAccessRightException {
        return "vuejsed";
    }
}

