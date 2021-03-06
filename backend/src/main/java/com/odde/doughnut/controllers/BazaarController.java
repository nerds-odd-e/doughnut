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
@RequestMapping("/bazaar")
public class BazaarController extends ApplicationMvcController {
    private final ModelFactoryService modelFactoryService;

    public BazaarController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("")
    public String bazaar(Model model) {
        BazaarModel bazaar = modelFactoryService.toBazaarModel();
        model.addAttribute("notebooks", bazaar.getAllNotebooks());
        return "bazaar/index";
    }

    @GetMapping("/notes/{note}")
    public String showBazaarNote(@PathVariable(name = "note") Integer noteId) throws NoAccessRightException {
        return "vuejsed";
    }

    @GetMapping("/articles/{note}")
    public String showBazaarNoteAsArticle(@PathVariable(name = "note") Note note, Model model) {
        return "bazaar/article";
    }
}
