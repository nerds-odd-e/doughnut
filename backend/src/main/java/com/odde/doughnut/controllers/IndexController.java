package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
public class IndexController {
    @RequestMapping(value = {
            "/",
            "/bazaar**",
            "/circles**",
            "/notebooks**",
            "/failure-report-list**",
            "/links**"
    })
    public String home() {
        return "vuejsed";
    }
}
