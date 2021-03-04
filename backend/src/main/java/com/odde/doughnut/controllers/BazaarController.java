package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class BazaarController {
    private final CurrentUserFetcher currentUserFetcher;
    private final ModelFactoryService modelFactoryService;

    public BazaarController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        this.currentUserFetcher = currentUserFetcher;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/bazaar")
    public String bazaar(Model model) {
        BazaarModel bazaar = modelFactoryService.toBazaarModel();
        model.addAttribute("notes", bazaar.getAllNotes());
        return "bazaar";
    }

    @PostMapping(value = "/notes/{note}/share")
    public RedirectView shareNote(@PathVariable("note") NoteEntity note) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(note);
        BazaarModel bazaar = modelFactoryService.toBazaarModel();
        bazaar.shareNote(note);
        return new RedirectView("/notes");
    }

}
