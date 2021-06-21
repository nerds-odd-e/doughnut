package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.factoryServices.ModelFactoryService;
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

    @GetMapping("/{subscription}")
    public String show(@PathVariable(name = "subscription") Subscription subscription) {
        return "subscriptions/show";
    }

    @GetMapping("/{subscription}/edit")
    public String edit(@PathVariable(name = "subscription") Subscription subscription) {
        return "subscriptions/edit";
    }

    @PostMapping("/{subscription}")
    @Transactional
    public String update(@Valid Subscription subscription, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "subscriptions/edit";
        }
        modelFactoryService.entityManager.persist(subscription);
        return "redirect:/subscriptions/" + subscription.getId();
    }

    @GetMapping("/notebooks/{notebook}/add_to_learning")
    public String addToLearning(@PathVariable(name = "notebook") Notebook notebook, Model model) {
        Subscription subscription = new Subscription();
        model.addAttribute("subscription", subscription);
        return "subscriptions/add_to_learning";
    }

    @PostMapping("/notebooks/{notebook}/subscribe")
    @Transactional
    public String createSubscription(@PathVariable(name = "notebook") Notebook notebook, @Valid Subscription subscription, BindingResult bindingResult, Model model) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "subscriptions/add_to_learning";
        }
        final UserModel userModel = currentUserFetcher.getUser();
        userModel.getAuthorization().assertReadAuthorization(notebook);
        subscription.setNotebook(notebook);
        subscription.setUser(userModel.getEntity());
        modelFactoryService.entityManager.persist(subscription);
        return "redirect:/subscriptions/" + subscription.getId();
    }

    @PostMapping("/{subscription}/delete")
    @Transactional
    public String destroySubscription(@Valid Subscription subscription) throws NoAccessRightException {
        final UserModel userModel = currentUserFetcher.getUser();
        userModel.getAuthorization().assertAuthorization(subscription);
        modelFactoryService.entityManager.remove(subscription);
        return "redirect:/notebooks/";
    }

}
