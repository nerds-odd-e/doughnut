package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

public class ApplicationMvcController {
    protected final CurrentUserFetcher currentUserFetcher;

    public ApplicationMvcController(CurrentUserFetcher currentUserFetcher) {
        this.currentUserFetcher = currentUserFetcher;
    }

    @ModelAttribute
    public void addAttributes(Model model) {
        model.addAttribute("currentUser", currentUserFetcher.getUser());
    }
}
