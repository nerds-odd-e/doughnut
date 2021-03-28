package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.SubscriptionEntity;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/bazaar_for_user")
public class BazaarForUserController extends ApplicationMvcController {
    private final ModelFactoryService modelFactoryService;

    public BazaarForUserController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/notes/{noteEntity}/add_to_learning")
    public String addToLearning(@PathVariable(name = "noteEntity") NoteEntity noteEntity, Model model) {
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        model.addAttribute("subscriptionEntity", subscriptionEntity);
        return "bazaar/add_to_learning";
    }

    @GetMapping("/notes/{noteEntity}/subscribe")
    public String createSubscription(@PathVariable(name = "noteEntity") NoteEntity noteEntity, @Valid SubscriptionEntity subscriptionEntity, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "bazaar/add_to_learning";
        }
        subscriptionEntity.setNoteEntity(noteEntity);
        subscriptionEntity.setUserEntity(currentUserFetcher.getUser().getEntity());
        modelFactoryService.entityManager.persist(subscriptionEntity);
        return "redirect:/notes/" + noteEntity.getId();
    }

}
