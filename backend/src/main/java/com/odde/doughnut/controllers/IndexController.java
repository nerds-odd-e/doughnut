package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class IndexController {
    private final CurrentUser currentUser;
    private final ModelFactoryService modelFactoryService;

    public IndexController(CurrentUser currentUser, ModelFactoryService modelFactoryService) {
        this.currentUser = currentUser;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        if (principal == null) {
            model.addAttribute("totalNotes", modelFactoryService.noteRepository.count());
            return "ask_to_login";
        }

        if (currentUser.getUser() == null) {
            model.addAttribute("user", new User());
            return "register";
        }

        return "index";
    }

}
