package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.SubscriptionEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;

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
        model.addAttribute("notes", bazaar.getAllNotes());
        return "bazaar/index";
    }

    @GetMapping("/notes/{noteEntity}")
    public String showBazaarNote(@PathVariable(name = "noteEntity") NoteEntity noteEntity, Model model) {
        model.addAttribute("treeNodeModel", modelFactoryService.toTreeNodeModel(noteEntity));
        return "bazaar/show";
    }

    @GetMapping("/articles/{noteEntity}")
    public String showBazaarNoteAsArticle(@PathVariable(name = "noteEntity") NoteEntity noteEntity, Model model) {
        model.addAttribute("treeNodeModel", modelFactoryService.toTreeNodeModel(noteEntity));
        return "bazaar/article";
    }
}
