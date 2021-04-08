package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.SubscriptionEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/subscriptions")
public class SubscriptionController extends ApplicationMvcController {
    private final ModelFactoryService modelFactoryService;

    public SubscriptionController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/{subscriptionEntity}")
    public String show(@PathVariable(name = "subscriptionEntity") SubscriptionEntity subscriptionEntity) {
        return "subscriptions/show";
    }

    @GetMapping("/{subscriptionEntity}/edit")
    public String edit(@PathVariable(name = "subscriptionEntity") SubscriptionEntity subscriptionEntity) {
        return "subscriptions/edit";
    }

    @PostMapping("/{subscriptionEntity}")
    @Transactional
    public String update(@Valid SubscriptionEntity subscriptionEntity, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "subscriptions/edit";
        }
        modelFactoryService.entityManager.persist(subscriptionEntity);
        return "redirect:/subscriptions/" + subscriptionEntity.getId();
    }

    @GetMapping("/notes/{noteEntity}/add_to_learning")
    public String addToLearning(@PathVariable(name = "noteEntity") NoteEntity noteEntity, Model model) {
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        model.addAttribute("subscriptionEntity", subscriptionEntity);
        return "subscriptions/add_to_learning";
    }

    @PostMapping("/notes/{noteEntity}/subscribe")
    @Transactional
    public String createSubscription(@PathVariable(name = "noteEntity") NoteEntity noteEntity, @Valid SubscriptionEntity subscriptionEntity, BindingResult bindingResult, Model model) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "subscriptions/add_to_learning";
        }
        if (modelFactoryService.bazaarNotebookRepository.findByNotebookEntity(noteEntity.getNotebookEntity()) == null) {
            throw new NoAccessRightException();
        }
        subscriptionEntity.setNotebookEntity(noteEntity.getNotebookEntity());
        subscriptionEntity.setUserEntity(currentUserFetcher.getUser().getEntity());
        modelFactoryService.entityManager.persist(subscriptionEntity);
        return "redirect:/subscriptions/" + subscriptionEntity.getId();
    }

}
