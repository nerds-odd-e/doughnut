package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.ModelForEntity;
import com.odde.doughnut.services.ModelFactoryService;
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
    public String failureReport(Model model) {
        Iterable<FailureReport> reports = modelFactoryService.failureReportRepository.findAll();
//        model.addAttribute("reports", reports);
        String[] hoge = new String[]{"1", "2","1", "2","1", "2","1", "2","1", "2","1", "2","1", "2","1", "2","1", "2","1", "2","1", "2","1", "2"};
        model.addAttribute("failureReports", hoge);
        return "failure-report-list/index";
    }

//    @GetMapping("/show/{id}")
//    public String show(@PathVariable(name = "id") FailureReport failureReport, Model model) {
////        FailureReportModel failureReportModel =  modelFactoryService.toFailureReportModel(failureReport);
////        model.addAttribute("reports", failureReportModel);
//        return "failure-report-list/show";
//    }
}

