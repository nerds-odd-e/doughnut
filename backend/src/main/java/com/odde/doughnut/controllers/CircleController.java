package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Controller
@RequestMapping("/circles")
public class CircleController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;


    public CircleController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("")
    public String index(Model model) {
        UserModel user = currentUserFetcher.getUser();
        model.addAttribute("user", user.getEntity());
        model.addAttribute("circleJoiningByInvitation", new CircleJoiningByInvitation());
        return "circles/index";
    }

    @GetMapping("/new")
    public String newCircle(Model model) {
        model.addAttribute("circle", new Circle());
        return "circles/new";
    }

    @PostMapping("")
    public String createCircle(@Valid Circle circle, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "circles/new";
        }
        UserModel userModel = currentUserFetcher.getUser();
        CircleModel circleModel = modelFactoryService.toCircleModel(circle);
        circleModel.joinAndSave(userModel);
        return "redirect:/circles/" + circle.getId();
    }

    @GetMapping("/{circle}")
    public String showCircle(@PathVariable("circle") Circle circle, Model model) throws NoAccessRightException {
        currentUserFetcher.getUser().getAuthorization().assertAuthorization(circle);
        model.addAttribute("notebooks", circle.getOwnership().getNotebooks());
        return "vuejsed";
    }

    @GetMapping("/join/{code}")
    public String joining(@PathVariable("code") String code) {
        return "vuejsed";
    }

    @PostMapping("/join")
    @Transactional
    public String joinCircle(@Valid CircleController.CircleJoiningByInvitation circleJoiningByInvitation, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "circles/join";
        }
        CircleModel circleModel = modelFactoryService.findCircleByInvitationCode(circleJoiningByInvitation.getInvitationCode());
        if (circleModel == null) {
            bindingResult.rejectValue("invitationCode", "error.error", "Does not match any circle");
            return "circles/join";
        }
        UserModel userModel = currentUserFetcher.getUser();
        if (userModel.getEntity().inCircle(circleModel.getEntity())) {
            bindingResult.rejectValue("invitationCode", "error.error", "You are already in this circle");
            return "circles/join";
        }
        circleModel.joinAndSave(userModel);
        return "redirect:/circles/" + circleModel.getEntity().getId();
    }

    @GetMapping("/{circle}/notebooks/new")
    public String newNoteInCircle(@PathVariable("circle") Circle circle, Model model) {
        model.addAttribute("ownership", circle.getOwnership());
        model.addAttribute("noteContent", new NoteContent());
        return "notebooks/new";
    }

    public static class CircleJoiningByInvitation {
        @NotNull
        @Size(min = 10, max = 20)
        @Getter
        @Setter
        String invitationCode;
    }
}
