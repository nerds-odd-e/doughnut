package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class IndexController extends ApplicationMvcController {
    private final ModelFactoryService modelFactoryService;

    public IndexController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        if (principal == null) {
            model.addAttribute("totalNotes", modelFactoryService.noteRepository.count());
            return "ask_to_login";
        }

        if (currentUserFetcher.getUser() == null) {
            model.addAttribute("user", new User());
            return "register";
        }

        return "index";
    }

}
