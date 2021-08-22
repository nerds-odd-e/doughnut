package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(value = {"/bazaar", "/circles", "/failure-report-list"})
public class BazaarController {

    @RequestMapping("**")
    public String any() {
        return "vuejsed";
    }

}
